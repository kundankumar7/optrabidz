package com.project.optrabidz.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

final class DiagramPublicationValidator {

    private static final Path CATALOG = Path.of(
            "docs", "architecture", "diagram-publication",
            "diagram-publications.json");
    private static final Path MERMAID_CONFIG = Path.of(
            "docs", "architecture", "diagram-publication",
            "mermaid-config.json");
    private static final int MINIMUM_PNG_WIDTH = 2000;
    private static final int MINIMUM_PNG_HEIGHT = 600;
    private static final Pattern MARKDOWN_IMAGE = Pattern.compile(
            "!\\[[^]]*]\\((?:<)?([^)>\\s]+)(?:>)?(?:\\s+[^)]*)?\\)");
    private static final Pattern HTML_IMAGE = Pattern.compile(
            "(?i)<img\\b[^>]*\\bsrc\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
    private static final Pattern DIRECTIONAL_CLASS = Pattern.compile(
            "(?is)\\.([a-z_][a-z0-9_-]*)\\s*\\{[^}]*?marker-end\\s*:");
    private static final Pattern PATH_TOKEN = Pattern.compile(
            "[A-Za-z]|[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?");
    private static final double SVG_EPSILON = 0.01;

    private DiagramPublicationValidator() {
    }

    static List<Violation> findViolations(Path repositoryRoot) throws Exception {
        Path root = repositoryRoot.toAbsolutePath().normalize();
        List<Violation> violations = new ArrayList<>();
        Path inventoryPath = root.resolve(CATALOG).normalize();

        if (!Files.isRegularFile(inventoryPath)) {
            return List.of(new Violation("catalogue", normalize(CATALOG),
                    "diagram publication catalogue does not exist"));
        }

        Inventory inventory;
        try {
            inventory = new ObjectMapper().readValue(inventoryPath.toFile(),
                    Inventory.class);
        } catch (IOException exception) {
            return List.of(new Violation("catalogue", normalize(CATALOG),
                    "diagram publication catalogue is invalid JSON"));
        }

        validateInventoryHeader(root, inventory, violations);
        Set<String> ids = new HashSet<>();
        Set<Path> canonicalAssets = new HashSet<>();

        if (inventory.diagrams() == null) {
            violations.add(new Violation("catalogue", normalize(CATALOG),
                    "diagram publication catalogue has no diagrams"));
        } else {
            for (DiagramEntry entry : inventory.diagrams()) {
                validateEntry(root, entry, ids, canonicalAssets, violations);
            }
        }

        findTemporaryAssets(root, violations);

        return violations.stream()
                .sorted(Comparator.comparing(Violation::diagramId)
                        .thenComparing(Violation::path)
                        .thenComparing(Violation::reason))
                .toList();
    }

    private static void validateInventoryHeader(Path root, Inventory inventory,
            List<Violation> violations) {
        if (inventory.schemaVersion() != 2) {
            violations.add(new Violation("catalogue", normalize(CATALOG),
                    "unsupported diagram publication schema version"));
        }
        Path config = root.resolve(MERMAID_CONFIG).normalize();
        if (!Files.isRegularFile(config)) {
            violations.add(new Violation("catalogue", relative(root, config),
                    "renderer configuration does not exist"));
        }
    }

    private static void validateEntry(Path root, DiagramEntry entry,
            Set<String> ids, Set<Path> canonicalAssets,
            List<Violation> violations) {
        String id = entry.id() == null || entry.id().isBlank()
                ? "unknown" : entry.id();
        if (!ids.add(id)) {
            violations.add(new Violation(id, normalize(CATALOG),
                    "diagram id is duplicated"));
        }

        Path owner = resolve(root, entry.primaryOwner(), id, violations);
        Path source = resolve(root, entry.source(), id, violations);
        Path svg = resolve(root, entry.svg(), id, violations);
        Path png = resolve(root, entry.png(), id, violations);

        validateSourceContract(root, id, entry, source, svg, violations);

        requireFile(root, id, owner, "owner document does not exist", violations);
        requireFile(root, id, source, "editable source does not exist", violations);
        requireFile(root, id, svg, "declared SVG does not exist", violations);

        if (svg != null && !canonicalAssets.add(svg)) {
            violations.add(new Violation(id, relative(root, svg),
                    "canonical SVG is assigned to more than one diagram"));
        }
        if (owner != null && svg != null && Files.isRegularFile(owner)
                && Files.isRegularFile(svg)) {
            validateDocumentEmbed(root, id, owner, svg,
                    "primary owner does not embed the declared SVG", violations);
        }
        if (entry.consumers() != null && svg != null && Files.isRegularFile(svg)) {
            for (String consumerPath : entry.consumers()) {
                Path consumer = resolve(root, consumerPath, id, violations);
                requireFile(root, id, consumer,
                        "consumer document does not exist", violations);
                if (consumer != null && Files.isRegularFile(consumer)) {
                    validateDocumentEmbed(root, id, consumer, svg,
                            "consumer does not embed the declared SVG", violations);
                }
            }
        }
        if (svg != null && Files.isRegularFile(svg)) {
            validateSvg(root, id, svg, violations);
        }

        if (png != null) {
            requireFile(root, id, png, "declared PNG does not exist",
                    violations);
            if (Files.isRegularFile(png)) {
                validatePng(root, id, png, violations);
            }
        }
    }

    private static void validateSourceContract(Path root, String id,
            DiagramEntry entry, Path source, Path svg,
            List<Violation> violations) {
        SourceType sourceType;
        try {
            sourceType = SourceType.valueOf(entry.sourceType());
        } catch (RuntimeException exception) {
            violations.add(new Violation(id, normalize(CATALOG),
                    "diagram source type is unsupported"));
            return;
        }

        if (sourceType == SourceType.CURATED_SVG && source != null && svg != null
                && !source.equals(svg)) {
            violations.add(new Violation(id, relative(root, source),
                    "curated SVG source must equal the published SVG"));
        }
        if (sourceType == SourceType.MERMAID_FILE && source != null
                && !source.getFileName().toString().toLowerCase(Locale.ROOT)
                        .endsWith(".mmd")) {
            violations.add(new Violation(id, relative(root, source),
                    "Mermaid source must use the .mmd extension"));
        }
    }

    private static void validateDocumentEmbed(Path root, String id, Path owner,
            Path svg, String reason, List<Violation> violations) {
        try {
            String markdown = Files.readString(owner);
            List<String> targets = new ArrayList<>();
            collectTargets(markdown, MARKDOWN_IMAGE, targets);
            collectTargets(markdown, HTML_IMAGE, targets);
            boolean embedded = targets.stream()
                    .map(DiagramPublicationValidator::stripFragmentAndQuery)
                    .filter(target -> !isExternal(target))
                    .map(target -> owner.getParent().resolve(target).normalize())
                    .anyMatch(svg::equals);
            if (!embedded) {
                violations.add(new Violation(id, relative(root, owner), reason));
            }
        } catch (IOException exception) {
            violations.add(new Violation(id, relative(root, owner),
                    "owner document could not be read"));
        }
    }

    private static void collectTargets(String markdown, Pattern pattern,
            List<String> targets) {
        Matcher matcher = pattern.matcher(markdown);
        while (matcher.find()) {
            targets.add(matcher.group(1));
        }
    }

    private static void validateSvg(Path root, String id, Path svg,
            List<Violation> violations) {
        try {
            String raw = Files.readString(svg);
            Document document = secureDocumentBuilderFactory()
                    .newDocumentBuilder().parse(svg.toFile());
            Element rootElement = document.getDocumentElement();

            if (!rootElement.hasAttribute("viewBox")) {
                violations.add(new Violation(id, relative(root, svg),
                        "SVG is missing viewBox"));
            }
            if (!hasNonBlankDirectChild(rootElement, "title")) {
                violations.add(new Violation(id, relative(root, svg),
                        "SVG is missing title"));
            }
            if (!hasNonBlankDirectChild(rootElement, "desc")) {
                violations.add(new Violation(id, relative(root, svg),
                        "SVG is missing description"));
            }
            if (!hasExplicitBackground(rootElement)) {
                violations.add(new Violation(id, relative(root, svg),
                        "SVG is missing an explicit background"));
            }
            if (document.getElementsByTagName("foreignObject").getLength() > 0) {
                violations.add(new Violation(id, relative(root, svg),
                        "SVG contains forbidden foreignObject"));
            }
            if (document.getElementsByTagName("script").getLength() > 0) {
                violations.add(new Violation(id, relative(root, svg),
                        "SVG contains forbidden script"));
            }
            if (containsExternalReference(document) || raw.contains("@import")) {
                violations.add(new Violation(id, relative(root, svg),
                        "SVG contains forbidden external reference"));
            }
            validateDirectionalConnectors(root, id, svg, document, violations);
        } catch (Exception exception) {
            violations.add(new Violation(id, relative(root, svg),
                    "SVG is not valid secure XML"));
        }
    }

    private static void validateDirectionalConnectors(Path root, String id,
            Path svg, Document document, List<Violation> violations) {
        Set<String> directionalClasses = directionalClasses(document);
        NodeList paths = document.getElementsByTagName("path");
        for (int index = 0; index < paths.getLength(); index++) {
            Element path = (Element) paths.item(index);
            if (!isDirectional(path, directionalClasses)) {
                continue;
            }

            String targetId = path.getAttribute("data-target").trim();
            if (targetId.isBlank()) {
                violations.add(new Violation(id, relative(root, svg),
                        "directional connector is missing data-target"));
                continue;
            }

            Element target = findElementById(document, targetId);
            if (target == null || !"rect".equals(target.getLocalName())) {
                violations.add(new Violation(id, relative(root, svg),
                        "directional connector data-target must reference a rectangle"));
                continue;
            }

            Segment finalSegment = finalOrthogonalSegment(path.getAttribute("d"));
            if (finalSegment == null) {
                violations.add(new Violation(id, relative(root, svg),
                        "directional connector must use an orthogonal path"));
                continue;
            }

            Rectangle rectangle = rectangle(target);
            if (!rectangle.containsOnBoundary(finalSegment.end())) {
                violations.add(new Violation(id, relative(root, svg),
                        "directional connector does not end on its declared target"));
            } else if (!rectangle.isPerpendicularEntry(finalSegment)) {
                violations.add(new Violation(id, relative(root, svg),
                        "directional connector must enter its target perpendicularly"));
            }
        }
    }

    private static Set<String> directionalClasses(Document document) {
        Set<String> classes = new HashSet<>();
        NodeList styles = document.getElementsByTagName("style");
        for (int index = 0; index < styles.getLength(); index++) {
            Matcher matcher = DIRECTIONAL_CLASS.matcher(
                    styles.item(index).getTextContent());
            while (matcher.find()) {
                classes.add(matcher.group(1));
            }
        }
        return classes;
    }

    private static boolean isDirectional(Element path,
            Set<String> directionalClasses) {
        String markerEnd = path.getAttribute("marker-end").trim();
        if (!markerEnd.isBlank() && !"none".equalsIgnoreCase(markerEnd)) {
            return true;
        }
        for (String className : path.getAttribute("class").trim().split("\\s+")) {
            if (directionalClasses.contains(className)) {
                return true;
            }
        }
        return false;
    }

    private static Element findElementById(Document document, String id) {
        NodeList elements = document.getElementsByTagName("*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (id.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        return null;
    }

    private static Rectangle rectangle(Element element) {
        return new Rectangle(
                number(element, "x"), number(element, "y"),
                number(element, "width"), number(element, "height"));
    }

    private static double number(Element element, String attribute) {
        String value = element.getAttribute(attribute).trim();
        return value.isBlank() ? 0 : Double.parseDouble(value);
    }

    private static Segment finalOrthogonalSegment(String pathData) {
        List<String> tokens = PATH_TOKEN.matcher(pathData).results()
                .map(result -> result.group())
                .toList();
        Point current = new Point(0, 0);
        Point previous = null;
        char command = 0;
        int index = 0;

        while (index < tokens.size()) {
            String token = tokens.get(index);
            if (Character.isLetter(token.charAt(0))) {
                command = token.charAt(0);
                index++;
                if (command == 'Z' || command == 'z') {
                    return null;
                }
                continue;
            }

            Point next;
            switch (command) {
                case 'M', 'L' -> {
                    if (index + 1 >= tokens.size()) {
                        return null;
                    }
                    next = new Point(Double.parseDouble(tokens.get(index)),
                            Double.parseDouble(tokens.get(index + 1)));
                    index += 2;
                }
                case 'm', 'l' -> {
                    if (index + 1 >= tokens.size()) {
                        return null;
                    }
                    next = new Point(current.x()
                            + Double.parseDouble(tokens.get(index)), current.y()
                            + Double.parseDouble(tokens.get(index + 1)));
                    index += 2;
                }
                case 'H' -> {
                    next = new Point(Double.parseDouble(token), current.y());
                    index++;
                }
                case 'h' -> {
                    next = new Point(current.x() + Double.parseDouble(token),
                            current.y());
                    index++;
                }
                case 'V' -> {
                    next = new Point(current.x(), Double.parseDouble(token));
                    index++;
                }
                case 'v' -> {
                    next = new Point(current.x(),
                            current.y() + Double.parseDouble(token));
                    index++;
                }
                default -> {
                    return null;
                }
            }
            previous = current;
            current = next;
        }

        return previous == null ? null : new Segment(previous, current);
    }

    private static DocumentBuilderFactory secureDocumentBuilderFactory()
            throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private static boolean hasExplicitBackground(Element svg) {
        String rootStyle = svg.getAttribute("style").toLowerCase(Locale.ROOT)
                .replace(" ", "");
        if (rootStyle.contains("background-color:")
                && !rootStyle.contains("background-color:transparent")) {
            return true;
        }

        NodeList children = svg.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index) instanceof Element element
                    && "rect".equals(element.getLocalName())) {
                String fill = element.getAttribute("fill").trim()
                        .toLowerCase(Locale.ROOT);
                if (isOpaqueFill(fill)
                        || hasOpaqueInlineFill(element.getAttribute("style"))
                        || hasOpaqueClassFill(svg, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasNonBlankDirectChild(Element parent,
            String tagName) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index) instanceof Element element
                    && tagName.equals(element.getLocalName())
                    && !element.getTextContent().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOpaqueInlineFill(String style) {
        Matcher matcher = Pattern.compile(
                "(?i)(?:^|;)\\s*fill\\s*:\\s*([^;]+)")
                .matcher(style);
        return matcher.find() && isOpaqueFill(matcher.group(1));
    }

    private static boolean hasOpaqueClassFill(Element svg, Element background) {
        String classNames = background.getAttribute("class").trim();
        if (classNames.isBlank()) {
            return false;
        }

        StringBuilder css = new StringBuilder();
        NodeList styles = svg.getElementsByTagName("style");
        for (int index = 0; index < styles.getLength(); index++) {
            css.append(styles.item(index).getTextContent()).append('\n');
        }

        for (String className : classNames.split("\\s+")) {
            Pattern rule = Pattern.compile(
                    "(?is)\\." + Pattern.quote(className)
                            + "\\s*\\{[^}]*?\\bfill\\s*:\\s*([^;}]+)");
            Matcher matcher = rule.matcher(css);
            if (matcher.find() && isOpaqueFill(matcher.group(1))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOpaqueFill(String fill) {
        String normalized = fill == null ? ""
                : fill.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        if (normalized.isBlank() || "none".equals(normalized)
                || "transparent".equals(normalized)) {
            return false;
        }
        if (normalized.matches("#[0-9a-f]{4}")) {
            return normalized.endsWith("f");
        }
        if (normalized.matches("#[0-9a-f]{8}")) {
            return normalized.endsWith("ff");
        }
        if (normalized.startsWith("rgba(")
                || normalized.startsWith("hsla(")) {
            int separator = normalized.lastIndexOf(',');
            return separator >= 0 && isFullyOpaqueAlpha(
                    normalized.substring(separator + 1,
                            normalized.length() - 1));
        }
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && normalized.endsWith(")")) {
            return isFullyOpaqueAlpha(
                    normalized.substring(slash + 1, normalized.length() - 1));
        }
        return true;
    }

    private static boolean isFullyOpaqueAlpha(String alpha) {
        return "1".equals(alpha) || "1.0".equals(alpha)
                || "100%".equals(alpha);
    }

    private static boolean containsExternalReference(Document document) {
        NodeList elements = document.getElementsByTagName("*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            String href = element.getAttribute("href");
            String xlinkHref = element.getAttributeNS(
                    "http://www.w3.org/1999/xlink", "href");
            if (isExternalSvgReference(href) || isExternalSvgReference(xlinkHref)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExternalSvgReference(String value) {
        return value != null && !value.isBlank() && !value.startsWith("#");
    }

    private static void validatePng(Path root, String id, Path png,
            List<Violation> violations) {
        try {
            BufferedImage image = ImageIO.read(png.toFile());
            if (image == null) {
                violations.add(new Violation(id, relative(root, png),
                        "declared PNG is not a readable PNG"));
                return;
            }
            if (image.getWidth() < MINIMUM_PNG_WIDTH) {
                violations.add(new Violation(id, relative(root, png),
                        "PNG width must be at least 2000 pixels"));
            }
            if (image.getHeight() < MINIMUM_PNG_HEIGHT) {
                violations.add(new Violation(id, relative(root, png),
                        "PNG height must be at least 600 pixels"));
            }
            if (containsTransparentPixel(image)) {
                violations.add(new Violation(id, relative(root, png),
                        "PNG contains transparent pixels"));
            }
        } catch (IOException exception) {
            violations.add(new Violation(id, relative(root, png),
                    "declared PNG could not be read"));
        }
    }

    private static boolean containsTransparentPixel(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return false;
        }
        int[] row = new int[image.getWidth()];
        for (int y = 0; y < image.getHeight(); y++) {
            image.getRGB(0, y, image.getWidth(), 1, row, 0, image.getWidth());
            for (int pixel : row) {
                if ((pixel >>> 24) != 0xFF) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void findTemporaryAssets(Path root,
            List<Violation> violations) throws IOException {
        Path docs = root.resolve("docs");
        if (!Files.isDirectory(docs)) {
            return;
        }
        try (var paths = Files.walk(docs)) {
            paths.filter(Files::isRegularFile)
                    .filter(DiagramPublicationValidator::isTemporaryAsset)
                    .forEach(path -> violations.add(new Violation(
                            "repository", relative(root, path),
                            "temporary clipboard asset is published")));
        }
    }

    private static boolean isTemporaryAsset(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.contains("clipboard-");
    }

    private static void requireFile(Path root, String id, Path path,
            String reason, List<Violation> violations) {
        if (path != null && !Files.isRegularFile(path)) {
            violations.add(new Violation(id, relative(root, path), reason));
        }
    }

    private static Path resolve(Path root, String rawPath, String id,
            List<Violation> violations) {
        if (rawPath == null || rawPath.isBlank()) {
            violations.add(new Violation(id, normalize(CATALOG),
                    "required catalogue path is missing"));
            return null;
        }
        Path resolved = root.resolve(rawPath).normalize();
        if (!resolved.startsWith(root)) {
            violations.add(new Violation(id, rawPath,
                    "path escapes repository root"));
            return null;
        }
        return resolved;
    }

    private static String stripFragmentAndQuery(String target) {
        int query = target.indexOf('?');
        int fragment = target.indexOf('#');
        int cut = target.length();
        if (query >= 0) {
            cut = Math.min(cut, query);
        }
        if (fragment >= 0) {
            cut = Math.min(cut, fragment);
        }
        return target.substring(0, cut);
    }

    private static boolean isExternal(String target) {
        String lower = target.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("mailto:") || lower.startsWith("data:");
    }

    private static String relative(Path root, Path path) {
        return normalize(root.relativize(path));
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    record Inventory(int schemaVersion, List<DiagramEntry> diagrams) {
    }

    enum SourceType {
        MERMAID_FILE,
        CURATED_SVG
    }

    record DiagramEntry(String id, String sourceType, String source,
                        String svg, String png, String primaryOwner,
                        List<String> consumers) {
    }

    record Violation(String diagramId, String path, String reason) {
    }

    private record Point(double x, double y) {
    }

    private record Segment(Point start, Point end) {
    }

    private record Rectangle(double x, double y, double width, double height) {

        boolean containsOnBoundary(Point point) {
            boolean onHorizontal = between(point.x(), x, x + width)
                    && (same(point.y(), y) || same(point.y(), y + height));
            boolean onVertical = between(point.y(), y, y + height)
                    && (same(point.x(), x) || same(point.x(), x + width));
            return onHorizontal || onVertical;
        }

        boolean isPerpendicularEntry(Segment segment) {
            Point end = segment.end();
            boolean verticalSegment = same(segment.start().x(), end.x())
                    && !same(segment.start().y(), end.y());
            boolean horizontalSegment = same(segment.start().y(), end.y())
                    && !same(segment.start().x(), end.x());
            boolean horizontalEdge = same(end.y(), y)
                    || same(end.y(), y + height);
            boolean verticalEdge = same(end.x(), x)
                    || same(end.x(), x + width);
            return (horizontalEdge && verticalSegment)
                    || (verticalEdge && horizontalSegment);
        }

        private static boolean between(double value, double start, double end) {
            return value >= start - SVG_EPSILON
                    && value <= end + SVG_EPSILON;
        }

        private static boolean same(double left, double right) {
            return Math.abs(left - right) <= SVG_EPSILON;
        }
    }
}

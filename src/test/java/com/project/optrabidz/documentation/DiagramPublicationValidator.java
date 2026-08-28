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

    private static final Path INVENTORY = Path.of(
            "docs", "architecture", "diagram-publication", "inventory.json");
    private static final int MINIMUM_PNG_WIDTH = 2000;
    private static final int MINIMUM_PNG_HEIGHT = 600;
    private static final Pattern MARKDOWN_IMAGE = Pattern.compile(
            "!\\[[^]]*]\\((?:<)?([^)>\\s]+)(?:>)?(?:\\s+[^)]*)?\\)");
    private static final Pattern HTML_IMAGE = Pattern.compile(
            "(?i)<img\\b[^>]*\\bsrc\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");

    private DiagramPublicationValidator() {
    }

    static List<Violation> findViolations(Path repositoryRoot) throws Exception {
        Path root = repositoryRoot.toAbsolutePath().normalize();
        List<Violation> violations = new ArrayList<>();
        Path inventoryPath = root.resolve(INVENTORY).normalize();

        if (!Files.isRegularFile(inventoryPath)) {
            return List.of(new Violation("inventory", normalize(INVENTORY),
                    "diagram inventory does not exist"));
        }

        Inventory inventory;
        try {
            inventory = new ObjectMapper().readValue(inventoryPath.toFile(),
                    Inventory.class);
        } catch (IOException exception) {
            return List.of(new Violation("inventory", normalize(INVENTORY),
                    "diagram inventory is invalid JSON"));
        }

        validateInventoryHeader(root, inventory, violations);
        Set<String> ids = new HashSet<>();
        Set<Path> githubAssets = new HashSet<>();

        if (inventory.diagrams() == null) {
            violations.add(new Violation("inventory", normalize(INVENTORY),
                    "diagram inventory has no diagrams"));
        } else {
            for (DiagramEntry entry : inventory.diagrams()) {
                validateEntry(root, entry, ids, githubAssets, violations);
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
        if (inventory.schemaVersion() != 1) {
            violations.add(new Violation("inventory", normalize(INVENTORY),
                    "unsupported diagram inventory schema version"));
        }
        if (inventory.renderer() == null) {
            violations.add(new Violation("inventory", normalize(INVENTORY),
                    "diagram renderer definition is missing"));
            return;
        }
        Path config = resolve(root, inventory.renderer().config(), "inventory",
                violations);
        if (config != null && !Files.isRegularFile(config)) {
            violations.add(new Violation("inventory", relative(root, config),
                    "renderer configuration does not exist"));
        }
    }

    private static void validateEntry(Path root, DiagramEntry entry,
            Set<String> ids, Set<Path> githubAssets,
            List<Violation> violations) {
        String id = entry.id() == null || entry.id().isBlank()
                ? "unknown" : entry.id();
        if (!ids.add(id)) {
            violations.add(new Violation(id, normalize(INVENTORY),
                    "diagram id is duplicated"));
        }

        Path owner = resolve(root, entry.owner(), id, violations);
        Path source = resolve(root, entry.source(), id, violations);
        Path svg = resolve(root, entry.githubSvg(), id, violations);
        Path png = entry.jiraPng() == null ? null
                : resolve(root, entry.jiraPng(), id, violations);

        validateSourceContract(root, id, entry, source, svg, violations);

        requireFile(root, id, owner, "owner document does not exist", violations);
        requireFile(root, id, source, "editable source does not exist", violations);
        requireFile(root, id, svg, "declared SVG does not exist", violations);

        if (svg != null && !githubAssets.add(svg)) {
            violations.add(new Violation(id, relative(root, svg),
                    "GitHub SVG is assigned to more than one diagram"));
        }
        if (owner != null && svg != null && Files.isRegularFile(owner)
                && Files.isRegularFile(svg)) {
            validateOwnerEmbed(root, id, owner, svg, violations);
        }
        if (svg != null && Files.isRegularFile(svg)) {
            validateSvg(root, id, svg, violations);
        }

        if (entry.jiraPngRequired() && png == null) {
            violations.add(new Violation(id, normalize(INVENTORY),
                    "required Jira PNG is not declared"));
        }
        if (png != null) {
            requireFile(root, id, png, "declared Jira PNG does not exist",
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
            violations.add(new Violation(id, normalize(INVENTORY),
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

    private static void validateOwnerEmbed(Path root, String id, Path owner,
            Path svg, List<Violation> violations) {
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
                violations.add(new Violation(id, relative(root, owner),
                        "owner document does not embed the declared SVG"));
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
        } catch (Exception exception) {
            violations.add(new Violation(id, relative(root, svg),
                    "SVG is not valid secure XML"));
        }
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
                        "declared Jira PNG is not a readable PNG"));
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
                    "declared Jira PNG could not be read"));
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
            violations.add(new Violation(id, normalize(INVENTORY),
                    "required inventory path is missing"));
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

    record Inventory(int schemaVersion, Renderer renderer,
                     List<DiagramEntry> diagrams) {
    }

    record Renderer(String packageName, String version, String config) {
    }

    enum SourceType {
        MERMAID_FILE,
        CURATED_SVG
    }

    record DiagramEntry(String id, String owner, String source,
                        String sourceType, String githubSvg, String jiraPng,
                        boolean jiraPngRequired, String remediation) {
    }

    record Violation(String diagramId, String path, String reason) {
    }
}

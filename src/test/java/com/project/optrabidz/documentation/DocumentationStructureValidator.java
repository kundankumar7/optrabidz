package com.project.optrabidz.documentation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DocumentationStructureValidator {

    private static final List<String> REQUIRED_ENTRIES = List.of(
            "docs/README.md",
            "docs/getting-started/README.md",
            "docs/architecture/README.md",
            "docs/api/README.md",
            "docs/database/README.md",
            "docs/security/README.md",
            "docs/operations/README.md",
            "docs/decisions/README.md");
    private static final Pattern FENCE = Pattern.compile("(?m)^\\s*```([^\\r\\n]*)$");
    private static final Pattern MARKDOWN_LINK = Pattern.compile(
            "(?<!!)\\[[^]]*]\\((?:<)?([^)>\\s]+)(?:>)?(?:\\s+[^)]*)?\\)");
    private static final Set<String> EXTERNAL_SCHEMES = Set.of(
            "http:", "https:", "mailto:", "data:");

    private DocumentationStructureValidator() {
    }

    static List<Violation> findViolations(Path repositoryRoot) throws IOException {
        Path root = repositoryRoot.toAbsolutePath().normalize();
        List<Violation> violations = new ArrayList<>();

        for (String required : REQUIRED_ENTRIES) {
            if (!Files.isRegularFile(root.resolve(required))) {
                violations.add(new Violation(required,
                        "required documentation entry does not exist"));
            }
        }

        List<Path> markdown = new ArrayList<>();
        Path readme = root.resolve("README.md");
        if (Files.isRegularFile(readme)) {
            markdown.add(readme);
        }
        Path docs = root.resolve("docs");
        if (Files.isDirectory(docs)) {
            try (var paths = Files.walk(docs)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString()
                                .toLowerCase(Locale.ROOT).endsWith(".md"))
                        .filter(path -> isReaderFacing(root, path))
                        .forEach(markdown::add);
            }
        }

        for (Path source : markdown) {
            inspect(root, source, violations);
        }

        return violations.stream()
                .sorted(Comparator.comparing(Violation::path)
                        .thenComparing(Violation::reason))
                .toList();
    }

    private static boolean isReaderFacing(Path root, Path path) {
        String relative = "/" + normalize(root.relativize(path)) + "/";
        return !relative.contains("/work-items/")
                && !relative.contains("/assets/");
    }

    private static void inspect(Path root, Path source,
            List<Violation> violations) throws IOException {
        String relative = normalize(root.relativize(source));
        String markdown = Files.readString(source);
        boolean workItem = relative.contains("/work-items/");

        Matcher fences = FENCE.matcher(markdown);
        while (fences.find()) {
            if ("mermaid".equalsIgnoreCase(fences.group(1).trim())) {
                violations.add(new Violation(relative,
                        "reader-facing Markdown contains a Mermaid fence"));
            }
        }

        String prose = stripFencedCode(markdown);
        Matcher links = MARKDOWN_LINK.matcher(prose);
        while (links.find()) {
            String target = stripFragmentAndQuery(links.group(1));
            if (isExternal(target)) {
                continue;
            }
            if (target.toLowerCase(Locale.ROOT).endsWith(".mmd")) {
                violations.add(new Violation(relative,
                        "reader-facing Markdown links Mermaid source"));
            }
            if (!workItem && target.replace('\\', '/').contains("work-items/")
                    && target.endsWith("implementation-plan.md")) {
                violations.add(new Violation(relative,
                        "stable documentation links a work-item implementation plan"));
            }
        }
    }

    private static String stripFencedCode(String markdown) {
        StringBuilder prose = new StringBuilder(markdown.length());
        boolean fenced = false;
        for (String line : markdown.split("\\R", -1)) {
            if (line.stripLeading().startsWith("```")) {
                fenced = !fenced;
                prose.append('\n');
            } else if (fenced) {
                prose.append('\n');
            } else {
                prose.append(line).append('\n');
            }
        }
        return prose.toString();
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
        return EXTERNAL_SCHEMES.stream().anyMatch(lower::startsWith);
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    record Violation(String path, String reason) {
    }
}

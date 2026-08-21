package com.project.optrabidz.documentation;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class DocumentationLinkValidator {

    private static final Pattern MARKDOWN_TARGET = Pattern.compile(
            "!?\\[[^]]*]\\((?:<([^>]+)>|([^\\s)]+))(?:\\s+[\\\"'][^\\\"']*[\\\"'])?\\)");
    private static final Pattern HTML_TARGET = Pattern.compile(
            "(?:href|src)\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FENCED_CODE = Pattern.compile("(?ms)^```.*?^```\\s*$");

    private DocumentationLinkValidator() {
    }

    static List<BrokenTarget> findBrokenTargets(Path repositoryRoot) throws IOException {
        Path root = repositoryRoot.toAbsolutePath().normalize();
        List<Path> markdownFiles = new ArrayList<>();
        Path rootReadme = root.resolve("README.md");
        if (Files.isRegularFile(rootReadme)) {
            markdownFiles.add(rootReadme);
        }
        Path docs = root.resolve("docs");
        if (Files.isDirectory(docs)) {
            try (Stream<Path> paths = Files.walk(docs)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                        .forEach(markdownFiles::add);
            }
        }

        List<BrokenTarget> broken = new ArrayList<>();
        for (Path source : markdownFiles) {
            String content = FENCED_CODE.matcher(Files.readString(source)).replaceAll("");
            collectTargets(MARKDOWN_TARGET.matcher(content), source, root, broken, true);
            collectTargets(HTML_TARGET.matcher(content), source, root, broken, false);
        }
        return List.copyOf(broken);
    }

    private static void collectTargets(
            Matcher matcher,
            Path source,
            Path root,
            List<BrokenTarget> broken,
            boolean markdown) {
        while (matcher.find()) {
            String target = markdown
                    ? (matcher.group(1) != null ? matcher.group(1) : matcher.group(2))
                    : matcher.group(1);
            validateTarget(source, root, target, broken);
        }
    }

    private static void validateTarget(
            Path source, Path root, String rawTarget, List<BrokenTarget> broken) {
        String lower = rawTarget.toLowerCase(Locale.ROOT);
        if (rawTarget.startsWith("#") || lower.startsWith("http://")
                || lower.startsWith("https://") || lower.startsWith("mailto:")
                || lower.startsWith("data:")) {
            return;
        }

        String pathOnly = rawTarget.split("[?#]", 2)[0];
        Path resolved = source.getParent()
                .resolve(URLDecoder.decode(pathOnly, StandardCharsets.UTF_8))
                .normalize()
                .toAbsolutePath();
        if (!resolved.startsWith(root)) {
            broken.add(new BrokenTarget(root.relativize(source), rawTarget, "escapes repository root"));
        } else if (!Files.exists(resolved)) {
            broken.add(new BrokenTarget(root.relativize(source), rawTarget, "target does not exist"));
        }
    }

    record BrokenTarget(Path source, String target, String reason) {
    }
}

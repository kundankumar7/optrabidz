package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ArchitectureModuleInventoryTest {

    private static final Path MAIN_ROOT = Path.of(
            "src", "main", "java", "com", "project", "optrabidz");
    private static final Path TEST_ROOT = Path.of(
            "src", "test", "java", "com", "project", "optrabidz");
    private static final Path INVENTORY = Path.of(
            "docs", "architecture", "modules", "inventory.json");
    private static final Pattern MODULE_IMPORT = Pattern.compile(
            "(?m)^import com\\.project\\.optrabidz\\.([a-z]+)\\.");
    private static final Pattern SECURITY_ADAPTER = Pattern.compile(
            "^(SecurityConfig|ActiveSessionFilter|CsrfCookieFilter|"
                    + "ProblemAuthenticationEntryPoint|ProblemAccessDeniedHandler|"
                    + "DocumentationSecurityConfiguration|DocumentationExposureValidator)\\.java$"
                    + "|SignatureVerifier|SecurityAuditor");

    @Test
    void inventoryCoversEveryModuleAndItsCurrentArchitectureSurfaces() throws Exception {
        JsonNode root = new ObjectMapper().readTree(INVENTORY.toFile());
        Map<String, JsonNode> modules = indexedModules(root.path("modules"));

        assertThat(modules.keySet())
                .as("inventory modules must match the top-level production packages")
                .containsExactlyElementsOf(topLevelModules());

        for (Map.Entry<String, JsonNode> entry : modules.entrySet()) {
            String module = entry.getKey();
            JsonNode definition = entry.getValue();
            Path sourceRoot = MAIN_ROOT.resolve(module);
            Path testRoot = TEST_ROOT.resolve(module);

            assertThat(definition.path("sourceRoot").asText())
                    .isEqualTo(normalize(sourceRoot));
            assertThat(definition.path("testRoot").asText())
                    .isEqualTo(normalize(testRoot));
            assertThat(definition.path("ownerPage").asText())
                    .isEqualTo("docs/architecture/modules/" + module + ".md");
            assertThat(definition.path("purpose").asText()).isNotBlank();

            List<Path> sourceFiles = javaFiles(sourceRoot);
            JsonNode surfaces = definition.path("surfaceCounts");
            assertThat(surfaces.path("sourceFiles").asInt()).isEqualTo(sourceFiles.size());
            assertThat(surfaces.path("httpBoundaries").asInt())
                    .isEqualTo(count(sourceFiles, this::isHttpBoundary));
            assertThat(surfaces.path("applicationServices").asInt())
                    .isEqualTo(count(sourceFiles, this::isApplicationService));
            assertThat(surfaces.path("repositories").asInt())
                    .isEqualTo(count(sourceFiles, this::isRepository));
            assertThat(surfaces.path("eventBoundaries").asInt())
                    .isEqualTo(count(sourceFiles, this::isEventBoundary));
            assertThat(surfaces.path("securityAdapters").asInt())
                    .isEqualTo(count(sourceFiles, this::isSecurityAdapter));
            assertThat(surfaces.path("tests").asInt())
                    .isEqualTo(javaFiles(testRoot).size());

            assertThat(textValues(definition.path("directDependencies")))
                    .as("direct dependencies for %s must match production imports", module)
                    .containsExactlyElementsOf(directDependencies(module, sourceFiles));
        }
    }

    private Map<String, JsonNode> indexedModules(JsonNode definitions) {
        Map<String, JsonNode> modules = new TreeMap<>();
        definitions.forEach(definition ->
                modules.put(definition.path("name").asText(), definition));
        return modules;
    }

    private Set<String> topLevelModules() throws IOException {
        try (Stream<Path> paths = Files.list(MAIN_ROOT)) {
            return paths.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        }
    }

    private List<Path> javaFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private int count(List<Path> paths, Predicate<Path> predicate) {
        return (int) paths.stream().filter(predicate).count();
    }

    private boolean isHttpBoundary(Path path) {
        return content(path).matches("(?s).*@RestController(?:Advice)?.*");
    }

    private boolean isApplicationService(Path path) {
        return normalize(path).contains("/application/")
                && path.getFileName().toString().endsWith("Service.java");
    }

    private boolean isRepository(Path path) {
        return normalize(path).contains("/repository/");
    }

    private boolean isEventBoundary(Path path) {
        String normalized = normalize(path);
        String source = content(path);
        return normalized.contains("/event/")
                || normalized.contains("/outbox/")
                || source.contains("implements OutboxEventProcessor")
                || source.contains("@TransactionalEventListener");
    }

    private boolean isSecurityAdapter(Path path) {
        return SECURITY_ADAPTER.matcher(path.getFileName().toString()).find();
    }

    private Set<String> directDependencies(String module, List<Path> sourceFiles) {
        Set<String> dependencies = new TreeSet<>();
        for (Path sourceFile : sourceFiles) {
            Matcher imports = MODULE_IMPORT.matcher(content(sourceFile));
            while (imports.find()) {
                String dependency = imports.group(1);
                if (!dependency.equals(module)) {
                    dependencies.add(dependency);
                }
            }
        }
        return dependencies;
    }

    private List<String> textValues(JsonNode values) {
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(value.asText()));
        return result;
    }

    private String content(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + path, exception);
        }
    }

    private String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }
}

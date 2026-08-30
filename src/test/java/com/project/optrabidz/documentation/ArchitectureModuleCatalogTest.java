package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ArchitectureModuleCatalogTest {

    private static final Path MAIN_ROOT = Path.of(
            "src", "main", "java", "com", "project", "optrabidz");
    private static final Path CATALOG = Path.of(
            "docs", "architecture", "module-catalog.json");
    private static final Path DEPENDENCY_GUIDE = Path.of(
            "docs", "architecture", "module-dependencies.md");
    private static final Set<String> CAPABILITIES = Set.of(
            "identity-access", "marketplace", "finance-payments", "platform-support");
    private static final Set<String> CATALOG_FIELDS = Set.of(
            "name", "capability", "ownerPage");
    private static final Pattern MODULE_IMPORT = Pattern.compile(
            "(?m)^import com\\.project\\.optrabidz\\.([a-z]+)\\.");

    @Test
    void catalogContainsOnlyIntentionalOwnershipForEveryProductionModule() throws Exception {
        JsonNode definitions = new ObjectMapper().readTree(CATALOG.toFile()).path("modules");
        Map<String, JsonNode> modules = new TreeMap<>();
        definitions.forEach(definition -> modules.put(definition.path("name").asText(), definition));

        assertThat(modules.keySet())
                .containsExactlyElementsOf(topLevelModules());

        for (Map.Entry<String, JsonNode> entry : modules.entrySet()) {
            String module = entry.getKey();
            JsonNode definition = entry.getValue();
            Set<String> fields = new TreeSet<>();
            definition.fieldNames().forEachRemaining(fields::add);

            assertThat(fields)
                    .as("catalog fields for %s", module)
                    .containsExactlyElementsOf(new TreeSet<>(CATALOG_FIELDS));
            assertThat(definition.path("capability").asText())
                    .as("capability for %s", module)
                    .isIn(CAPABILITIES);
            assertThat(definition.path("ownerPage").asText())
                    .isEqualTo("docs/architecture/modules/" + module + ".md");
        }
    }

    @Test
    void dependencyGuideMatchesCurrentProductionImports() throws Exception {
        String documentation = Files.readString(DEPENDENCY_GUIDE);

        for (String module : topLevelModules()) {
            Set<String> dependencies = directDependencies(module);
            String renderedDependencies = dependencies.isEmpty()
                    ? "None"
                    : dependencies.stream()
                            .map(dependency -> "`" + dependency + "`")
                            .collect(java.util.stream.Collectors.joining(", "));

            assertThat(documentation)
                    .as("documented dependencies for %s", module)
                    .contains("| `" + module + "` | " + renderedDependencies + " |");
        }
    }

    private Set<String> topLevelModules() throws IOException {
        try (Stream<Path> paths = Files.list(MAIN_ROOT)) {
            return paths.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        }
    }

    private Set<String> directDependencies(String module) throws IOException {
        Set<String> dependencies = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(MAIN_ROOT.resolve(module))) {
            for (Path source : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList()) {
                Matcher imports = MODULE_IMPORT.matcher(Files.readString(source));
                while (imports.find()) {
                    String dependency = imports.group(1);
                    if (!dependency.equals(module)) {
                        dependencies.add(dependency);
                    }
                }
            }
        }
        return dependencies;
    }
}

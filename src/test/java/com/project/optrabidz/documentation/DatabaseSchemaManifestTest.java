package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DatabaseSchemaManifestTest {

    private static final Path MIGRATION = Path.of(
            "src", "main", "resources", "db", "migration", "V1__baseline.sql");
    private static final Path MANIFEST = Path.of(
            "docs", "database", "schema-manifest.json");
    private static final Pattern TABLE = Pattern.compile(
            "(?ms)^CREATE TABLE(?: IF NOT EXISTS)?\\s+([a-z_][a-z0-9_]*)\\s*\\((.*?)^\\);$");
    private static final Pattern FOREIGN_KEY = Pattern.compile(
            "(?ms)CONSTRAINT\\s+([a-z_][a-z0-9_]*)\\s+"
                    + "FOREIGN KEY\\s*\\(([^)]+)\\)\\s+"
                    + "REFERENCES\\s+([a-z_][a-z0-9_]*)\\s*\\(([^)]+)\\)\\s+"
                    + "ON DELETE\\s+(RESTRICT|CASCADE|SET NULL|NO ACTION)");
    private static final Pattern CHECK = Pattern.compile(
            "(?m)CONSTRAINT\\s+([a-z_][a-z0-9_]*)\\s+CHECK\\b");
    private static final Pattern TABLE_UNIQUE = Pattern.compile(
            "(?m)^\\s*UNIQUE\\s*\\(([^)]+)\\)");
    private static final Pattern INLINE_UNIQUE = Pattern.compile(
            "(?m)^\\s*([a-z_][a-z0-9_]*)\\s+[^,\\n]*\\bUNIQUE\\b");
    private static final Pattern INDEX = Pattern.compile(
            "(?ms)^CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+([a-z_][a-z0-9_]*)"
                    + "\\s+ON\\s+([a-z_][a-z0-9_]*)\\b.*?;$");
    private static final Pattern TRIGGER = Pattern.compile(
            "(?ms)^CREATE TRIGGER\\s+([a-z_][a-z0-9_]*).*?"
                    + "\\bON\\s+([a-z_][a-z0-9_]*)\\b.*?;$");

    @Test
    void manifestMatchesTheExecutableFlywayBaseline() throws Exception {
        String migration = Files.readString(MIGRATION);
        JsonNode manifest = new ObjectMapper().readTree(MANIFEST.toFile());
        List<TableBlock> tables = tableBlocks(migration);

        assertThat(textSet(manifest.path("tables")))
                .containsExactlyElementsOf(tables.stream()
                        .map(TableBlock::name)
                        .collect(java.util.stream.Collectors.toCollection(TreeSet::new)));
        assertThat(foreignKeys(manifest.path("foreignKeys")))
                .containsExactlyElementsOf(derivedForeignKeys(tables));
        assertThat(namedObjects(manifest.path("checkConstraints")))
                .containsExactlyElementsOf(derivedChecks(tables));
        assertThat(columnSets(manifest.path("uniqueConstraints")))
                .containsExactlyElementsOf(derivedUniqueConstraints(tables));
        assertThat(namedObjects(manifest.path("partialIndexes")))
                .containsExactlyElementsOf(derivedPartialIndexes(migration));
        assertThat(namedObjects(manifest.path("triggers")))
                .containsExactlyElementsOf(derivedTriggers(migration));

        JsonNode summary = manifest.path("summary");
        assertThat(summary.path("tables").asInt()).isEqualTo(tables.size());
        assertThat(summary.path("foreignKeys").asInt()).isEqualTo(derivedForeignKeys(tables).size());
        assertThat(summary.path("uniqueConstraints").asInt()).isEqualTo(derivedUniqueConstraints(tables).size());
        assertThat(summary.path("checkConstraints").asInt()).isEqualTo(derivedChecks(tables).size());
        assertThat(summary.path("partialIndexes").asInt()).isEqualTo(derivedPartialIndexes(migration).size());
        assertThat(summary.path("triggers").asInt()).isEqualTo(derivedTriggers(migration).size());

        assertThat(manifest.path("correlations").size())
                .as("intentional non-FK relationships must be documented separately")
                .isGreaterThanOrEqualTo(5);
        manifest.path("correlations").forEach(correlation -> {
            assertThat(correlation.path("id").asText()).isNotBlank();
            assertThat(correlation.path("from").asText()).isNotBlank();
            assertThat(correlation.path("to").asText()).isNotBlank();
            assertThat(correlation.path("basis").asText()).isNotBlank();
        });
    }

    private List<TableBlock> tableBlocks(String migration) {
        List<TableBlock> result = new ArrayList<>();
        TABLE.matcher(migration).results().forEach(match ->
                result.add(new TableBlock(match.group(1), match.group(2))));
        return result;
    }

    private Set<String> derivedForeignKeys(List<TableBlock> tables) {
        Set<String> result = new TreeSet<>();
        for (TableBlock table : tables) {
            Matcher matcher = FOREIGN_KEY.matcher(table.body());
            while (matcher.find()) {
                List<String> childColumns = columns(matcher.group(2));
                boolean nullable = childColumns.stream()
                        .anyMatch(column -> isNullable(table.body(), column));
                result.add(String.join("|",
                        matcher.group(1), table.name(), String.join(",", childColumns),
                        matcher.group(3), String.join(",", columns(matcher.group(4))),
                        Boolean.toString(nullable), matcher.group(5)));
            }
        }
        return result;
    }

    private Set<String> foreignKeys(JsonNode values) {
        Set<String> result = new TreeSet<>();
        values.forEach(value -> result.add(String.join("|",
                value.path("name").asText(),
                value.path("childTable").asText(),
                String.join(",", textValues(value.path("childColumns"))),
                value.path("parentTable").asText(),
                String.join(",", textValues(value.path("parentColumns"))),
                Boolean.toString(value.path("nullable").asBoolean()),
                value.path("onDelete").asText())));
        return result;
    }

    private boolean isNullable(String body, String column) {
        Matcher matcher = Pattern.compile(
                "(?m)^\\s*" + Pattern.quote(column) + "\\s+([^,\\n]+)")
                .matcher(body);
        assertThat(matcher.find()).as("column %s must exist", column).isTrue();
        return !matcher.group(1).contains("NOT NULL")
                && !matcher.group(1).contains("PRIMARY KEY");
    }

    private Set<String> derivedChecks(List<TableBlock> tables) {
        Set<String> result = new TreeSet<>();
        for (TableBlock table : tables) {
            CHECK.matcher(table.body()).results()
                    .forEach(match -> result.add(match.group(1) + "|" + table.name()));
        }
        return result;
    }

    private Set<String> derivedUniqueConstraints(List<TableBlock> tables) {
        Set<String> result = new TreeSet<>();
        for (TableBlock table : tables) {
            INLINE_UNIQUE.matcher(table.body()).results()
                    .forEach(match -> result.add(table.name() + "|" + match.group(1)));
            TABLE_UNIQUE.matcher(table.body()).results()
                    .forEach(match -> result.add(table.name() + "|"
                            + String.join(",", columns(match.group(1)))));
        }
        return result;
    }

    private Set<String> derivedPartialIndexes(String migration) {
        Set<String> result = new TreeSet<>();
        INDEX.matcher(migration).results()
                .filter(match -> match.group().matches("(?s).*\\bWHERE\\b.*"))
                .forEach(match -> result.add(match.group(1) + "|" + match.group(2)));
        return result;
    }

    private Set<String> derivedTriggers(String migration) {
        Set<String> result = new TreeSet<>();
        TRIGGER.matcher(migration).results()
                .forEach(match -> result.add(match.group(1) + "|" + match.group(2)));
        return result;
    }

    private Set<String> namedObjects(JsonNode values) {
        Set<String> result = new TreeSet<>();
        values.forEach(value -> result.add(
                value.path("name").asText() + "|" + value.path("table").asText()));
        return result;
    }

    private Set<String> columnSets(JsonNode values) {
        Set<String> result = new TreeSet<>();
        values.forEach(value -> result.add(
                value.path("table").asText() + "|"
                        + String.join(",", textValues(value.path("columns")))));
        return result;
    }

    private Set<String> textSet(JsonNode values) {
        return new TreeSet<>(textValues(values));
    }

    private List<String> textValues(JsonNode values) {
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(value.asText()));
        return result;
    }

    private List<String> columns(String value) {
        return Pattern.compile("\\s*,\\s*").splitAsStream(value.trim()).toList();
    }

    private record TableBlock(String name, String body) {
    }
}

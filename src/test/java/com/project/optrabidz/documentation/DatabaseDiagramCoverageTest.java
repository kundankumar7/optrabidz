package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DatabaseDiagramCoverageTest {

    private static final Pattern MIGRATION_TABLE = Pattern.compile(
            "(?m)^CREATE TABLE(?: IF NOT EXISTS)?\\s+([a-z_][a-z0-9_]*)\\s*\\(");
    private static final Pattern DIAGRAM_ENTITY = Pattern.compile(
            "(?m)^ {4}([a-z_][a-z0-9_]*) \\{$");

    @Test
    void erDiagramSourceCoversEveryFlywayBaselineTable() throws Exception {
        String migration = Files.readString(Path.of(
                "src", "main", "resources", "db", "migration",
                "V1__baseline.sql"));
        String diagramSource = Files.readString(Path.of(
                "docs", "database", "assets", "er-diagram-source.md"));

        assertThat(matches(DIAGRAM_ENTITY, diagramSource))
                .as("ER diagram entities must match Flyway baseline tables")
                .containsExactlyElementsOf(matches(MIGRATION_TABLE, migration));
    }

    private static Set<String> matches(Pattern pattern, String content) {
        return pattern.matcher(content).results()
                .map(result -> result.group(1))
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
    }
}

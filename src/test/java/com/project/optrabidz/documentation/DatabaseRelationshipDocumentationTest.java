package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseRelationshipDocumentationTest {

    private static final Path DATABASE_ROOT = Path.of("docs", "database");

    @Test
    void focusedRelationshipRowsExposeExactForeignKeySemantics() throws Exception {
        JsonNode manifest = manifest();
        List<String> lines = Files.readAllLines(DATABASE_ROOT.resolve("er-diagram.md"));

        for (JsonNode foreignKey : manifest.path("foreignKeys")) {
            String childReference = foreignKey.path("childTable").asText() + "."
                    + foreignKey.path("childColumns").get(0).asText();
            String row = lines.stream()
                    .filter(line -> line.startsWith("| `R"))
                    .filter(line -> line.contains("`" + childReference + "`"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "No focused relationship row for " + childReference));

            assertThat(row)
                    .as("constraint name for %s", childReference)
                    .contains("`" + foreignKey.path("name").asText() + "`");
            assertThat(row)
                    .as("delete behavior for %s", childReference)
                    .contains("`ON DELETE " + foreignKey.path("onDelete").asText() + "`");
            if (foreignKey.path("nullable").asBoolean()) {
                assertThat(row).as("nullability for %s", childReference).contains("nullable `FK`");
            } else {
                assertThat(row).as("nullability for %s", childReference)
                        .containsAnyOf("`NOT NULL`", "composite `PK`");
            }
        }
    }

    @Test
    void intentionalCorrelationsAreListedSeparatelyFromForeignKeys() throws Exception {
        JsonNode manifest = manifest();
        String documentation = Files.readString(DATABASE_ROOT.resolve("er-diagram.md"));

        assertThat(documentation).contains("## Intentional non-FK correlations");
        for (JsonNode correlation : manifest.path("correlations")) {
            assertThat(documentation)
                    .as("correlation %s", correlation.path("id").asText())
                    .contains("| `" + correlation.path("id").asText() + "` |")
                    .contains("`" + correlation.path("from").asText() + "`")
                    .contains("`" + correlation.path("to").asText() + "`");
        }
    }

    private JsonNode manifest() throws Exception {
        return new ObjectMapper().readTree(
                DATABASE_ROOT.resolve("schema-manifest.json").toFile());
    }
}

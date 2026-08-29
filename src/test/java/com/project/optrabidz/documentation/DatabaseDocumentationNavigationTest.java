package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseDocumentationNavigationTest {

    private static final Path DATABASE_ROOT = Path.of("docs", "database");
    private static final List<String> JOURNEY_STAGES = List.of(
            "Identity and access",
            "Participant profiles",
            "Marketplace discovery and bidding",
            "Agreement and finance",
            "Payment execution",
            "Reliable supporting work");

    @Test
    void databaseEntryPointRoutesReadersToTheJourneyAndFocusedViews() throws Exception {
        String entryPoint = Files.readString(DATABASE_ROOT.resolve("README.md"));
        String journey = Files.readString(DATABASE_ROOT.resolve("relationship-journey.md"));
        String erIndex = Files.readString(DATABASE_ROOT.resolve("er-diagram.md"));

        assertThat(entryPoint).contains("(relationship-journey.md)");
        assertThat(entryPoint).contains("(er-diagram.md)");
        assertThat(erIndex).contains("## Choose a relationship view");

        for (String stage : JOURNEY_STAGES) {
            assertThat(journey).contains("## " + stage);
        }
    }

    @Test
    void relationalJourneyAccountsForEveryManifestTable() throws Exception {
        String journey = Files.readString(DATABASE_ROOT.resolve("relationship-journey.md"));
        JsonNode tables = new ObjectMapper()
                .readTree(DATABASE_ROOT.resolve("schema-manifest.json").toFile())
                .path("tables");

        tables.forEach(table -> assertThat(journey)
                .as("relational journey must place table %s", table.asText())
                .contains("`" + table.asText() + "`"));
    }
}

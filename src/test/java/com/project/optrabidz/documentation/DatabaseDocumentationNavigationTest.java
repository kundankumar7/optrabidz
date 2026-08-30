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
    private static final List<String> FOCUSED_VIEWS = List.of(
            "identity-access.md",
            "participant-profile.md",
            "marketplace-bidding.md",
            "agreement-acceptance.md",
            "settlement.md",
            "repayment-schedule.md",
            "payment-intent.md",
            "payment-processing.md",
            "payment-webhook.md",
            "notification-delivery.md",
            "outbox-audit.md");
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
        String viewIndex = Files.readString(DATABASE_ROOT.resolve("views/README.md"));

        assertThat(entryPoint).contains("(relationship-journey.md)");
        assertThat(entryPoint).contains("(views/README.md)");
        assertThat(entryPoint).contains("(reference/README.md)");
        assertThat(entryPoint).doesNotContain("schema-manifest.json");
        assertThat(viewIndex).contains("## Choose a relationship view");
        assertThat(journey).contains("assets/relationship-journey.svg");
        assertThat(DATABASE_ROOT.resolve("assets/relationship-journey.svg")).exists();
        assertThat(DATABASE_ROOT.resolve("assets/relationship-journey.png")).exists();

        for (String view : FOCUSED_VIEWS) {
            assertThat(viewIndex).contains("(" + view + ")");
            assertThat(DATABASE_ROOT.resolve("views").resolve(view)).exists();
        }

        assertThat(DATABASE_ROOT.resolve("er-diagram.md")).doesNotExist();

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

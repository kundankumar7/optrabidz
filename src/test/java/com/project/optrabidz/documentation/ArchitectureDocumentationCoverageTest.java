package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureDocumentationCoverageTest {

    private static final Path ARCHITECTURE_ROOT = Path.of("docs", "architecture");
    private static final Path MODULE_CATALOG = ARCHITECTURE_ROOT.resolve("module-catalog.json");
    private static final List<String> LAYERED_VIEWS = List.of(
            "system-context.md",
            "runtime.md",
            "capabilities/README.md",
            "modules/README.md",
            "module-dependencies.md",
            "flows/request-security.md",
            "flows/event-delivery.md",
            "flows/error-disclosure.md");
    private static final List<String> CAPABILITY_PAGES = List.of(
            "identity-access",
            "marketplace",
            "finance-payments",
            "platform-support");
    private static final List<String> MODULE_SECTIONS = List.of(
            "Purpose",
            "Entry points",
            "Application and domain",
            "Persistence",
            "Events",
            "Dependencies",
            "Security and errors",
            "Verification",
            "Known gaps");

    @Test
    void architectureEntryPointLinksEveryLayeredView() throws Exception {
        String entryPoint = Files.readString(ARCHITECTURE_ROOT.resolve("README.md"));

        for (String view : LAYERED_VIEWS) {
            assertThat(ARCHITECTURE_ROOT.resolve(view))
                    .as("layered architecture view %s", view)
                    .isRegularFile();
            assertThat(entryPoint)
                    .as("architecture entry point must link %s", view)
                    .contains("(" + view.replace('\\', '/') + ")");
        }
    }

    @Test
    void documentationPortalSeparatesUnderstandingFromChangeGuidance() throws Exception {
        String portal = Files.readString(Path.of("docs", "README.md"));

        assertThat(portal)
                .contains("## Understand the system")
                .contains("## Change or verify the system");
    }

    @Test
    void capabilityIndexLinksEveryApprovedCapabilityPage() throws Exception {
        Path capabilityRoot = ARCHITECTURE_ROOT.resolve("capabilities");
        String index = Files.readString(capabilityRoot.resolve("README.md"));

        for (String capability : CAPABILITY_PAGES) {
            assertThat(capabilityRoot.resolve(capability + ".md"))
                    .as("capability page %s", capability)
                    .isRegularFile();
            assertThat(index)
                    .as("capability index must link %s", capability)
                    .contains("(" + capability + ".md)");
        }
    }

    @Test
    void everyModuleHasAnOwnedPageWithTheReviewerSections() throws Exception {
        JsonNode modules = new ObjectMapper().readTree(MODULE_CATALOG.toFile()).path("modules");
        String moduleIndex = Files.readString(ARCHITECTURE_ROOT.resolve("modules/README.md"));

        for (JsonNode module : modules) {
            String name = module.path("name").asText();
            String capability = module.path("capability").asText();
            Path ownerPage = Path.of(module.path("ownerPage").asText());

            assertThat(ownerPage).as("owner page for %s", name).isRegularFile();
            assertThat(moduleIndex)
                    .as("module index must link %s", name)
                    .contains("(" + name + ".md)");

            String content = Files.readString(ownerPage);
            assertThat(content)
                    .as("%s module page must link its capability", name)
                    .contains("(../capabilities/" + capability + ".md)");
            for (String section : MODULE_SECTIONS) {
                assertThat(content)
                        .as("%s module page section %s", name, section)
                        .contains("## " + section);
            }
        }
    }
}

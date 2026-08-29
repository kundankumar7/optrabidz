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
    private static final Path INVENTORY = ARCHITECTURE_ROOT.resolve("modules/inventory.json");
    private static final List<String> LAYERED_VIEWS = List.of(
            "system-context.md",
            "runtime.md",
            "modules/README.md",
            "module-dependencies.md",
            "flows/request-security.md",
            "flows/event-delivery.md",
            "flows/error-disclosure.md");
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
    void everyModuleHasAnOwnedPageWithTheReviewerSections() throws Exception {
        JsonNode modules = new ObjectMapper().readTree(INVENTORY.toFile()).path("modules");
        String moduleIndex = Files.readString(ARCHITECTURE_ROOT.resolve("modules/README.md"));

        for (JsonNode module : modules) {
            String name = module.path("name").asText();
            Path ownerPage = Path.of(module.path("ownerPage").asText());

            assertThat(ownerPage).as("owner page for %s", name).isRegularFile();
            assertThat(moduleIndex)
                    .as("module index must link %s", name)
                    .contains("(" + name + ".md)");

            String content = Files.readString(ownerPage);
            for (String section : MODULE_SECTIONS) {
                assertThat(content)
                        .as("%s module page section %s", name, section)
                        .contains("## " + section);
            }
        }
    }
}

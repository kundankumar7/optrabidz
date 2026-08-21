package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentationLinkValidatorTest {

    @TempDir
    Path repository;

    @Test
    void reportsBrokenMarkdownAndHtmlTargets() throws Exception {
        Files.createDirectories(repository.resolve("docs"));
        Files.writeString(repository.resolve("README.md"),
                "[missing](docs/missing.md)\n<img src=\"docs/missing.png\">\n");

        assertThat(DocumentationLinkValidator.findBrokenTargets(repository))
                .extracting(DocumentationLinkValidator.BrokenTarget::target)
                .containsExactlyInAnyOrder("docs/missing.md", "docs/missing.png");
    }

    @Test
    void acceptsExistingEncodedTargetsAndIgnoresExternalTargets() throws Exception {
        Files.createDirectories(repository.resolve("docs/a folder"));
        Files.writeString(repository.resolve("docs/a folder/design.md"), "# Design\n");
        Files.writeString(repository.resolve("README.md"), """
                [design](docs/a%20folder/design.md#decision)
                [section](#local-section)
                [website](https://example.com)
                [email](mailto:team@example.com)
                """);

        assertThat(DocumentationLinkValidator.findBrokenTargets(repository)).isEmpty();
    }

    @Test
    void rejectsTargetsOutsideRepository() throws Exception {
        Files.createDirectories(repository.resolve("docs"));
        Files.writeString(repository.resolve("docs/index.md"), "[escape](../../secret.md)\n");

        assertThat(DocumentationLinkValidator.findBrokenTargets(repository))
                .singleElement()
                .satisfies(target -> assertThat(target.reason()).isEqualTo("escapes repository root"));
    }

    @Test
    void ignoresTargetsInsideFencedCodeExamples() throws Exception {
        Files.createDirectories(repository.resolve("docs"));
        String fence = "`".repeat(3);
        Files.writeString(repository.resolve("docs/plan.md"),
                fence + "markdown\n[example](missing-example.md)\n" + fence + "\n");

        assertThat(DocumentationLinkValidator.findBrokenTargets(repository)).isEmpty();
    }
}

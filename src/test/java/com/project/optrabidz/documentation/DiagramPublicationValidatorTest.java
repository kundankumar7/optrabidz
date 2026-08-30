package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiagramPublicationValidatorTest {

    @TempDir
    Path repository;

    @Test
    void acceptsNeutralPublicationWithOwnerAndConsumer() throws Exception {
        writeCatalog("""
                {
                  "schemaVersion": 2,
                  "diagrams": [{
                    "id": "public-error-flow",
                    "sourceType": "CURATED_SVG",
                    "source": "docs/assets/flow.svg",
                    "svg": "docs/assets/flow.svg",
                    "png": "docs/assets/flow.png",
                    "primaryOwner": "docs/errors.md",
                    "consumers": ["docs/architecture/error-flow.md"]
                  }]
                }
                """);
        write("docs/errors.md", "![Public error flow](assets/flow.svg)\n");
        write("docs/architecture/error-flow.md",
                "![Public error flow](../assets/flow.svg)\n");
        write("docs/assets/flow.svg", safeSvg());
        write("docs/architecture/diagram-publication/mermaid-config.json", "{}\n");
        writePng("docs/assets/flow.png", 2000, 600, 0xFFFFFFFF);

        assertThat(DiagramPublicationValidator.findViolations(repository)).isEmpty();
    }

    @Test
    void rejectsLegacyProductSpecificContract() throws Exception {
        writeCatalog("""
                {
                  "schemaVersion": 1,
                  "renderer": {"version": "11.16.0"},
                  "diagrams": [{
                    "id": "legacy",
                    "sourceType": "CURATED_SVG",
                    "source": "docs/assets/flow.svg",
                    "githubSvg": "docs/assets/flow.svg",
                    "jiraPng": "docs/assets/flow.png",
                    "jiraPngRequired": true,
                    "owner": "docs/errors.md",
                    "remediation": "PASS"
                  }]
                }
                """);

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .extracting(DiagramPublicationValidator.Violation::reason)
                .contains("diagram publication catalogue is invalid JSON");
    }

    @Test
    void requiresEveryConsumerToEmbedTheCanonicalSvg() throws Exception {
        writeCatalog(singleEntry("docs/consumer.md"));
        write("docs/owner.md", "![Flow](assets/flow.svg)\n");
        write("docs/consumer.md", "# Consumer without the figure\n");
        write("docs/assets/flow.svg", safeSvg());
        write("docs/architecture/diagram-publication/mermaid-config.json", "{}\n");
        writePng("docs/assets/flow.png", 2000, 600, 0xFFFFFFFF);

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .extracting(DiagramPublicationValidator.Violation::reason)
                .contains("consumer does not embed the declared SVG");
    }

    @Test
    void reportsUnsafeIncompleteAndUnreadableAssets() throws Exception {
        writeCatalog(singleEntry());
        write("docs/owner.md", "# Owner without the figure\n");
        write("docs/assets/flow.svg", """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <script>window.alert('unsafe')</script>
                  <foreignObject><div>HTML label</div></foreignObject>
                </svg>
                """);
        write("docs/architecture/diagram-publication/mermaid-config.json", "{}\n");
        writePng("docs/assets/flow.png", 100, 100, 0x00FFFFFF);
        writePng("docs/assets/clipboard-preview.png", 2400, 800, 0xFFFFFFFF);

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .extracting(DiagramPublicationValidator.Violation::reason)
                .contains(
                        "primary owner does not embed the declared SVG",
                        "SVG is missing viewBox",
                        "SVG is missing title",
                        "SVG is missing description",
                        "SVG is missing an explicit background",
                        "SVG contains forbidden foreignObject",
                        "SVG contains forbidden script",
                        "PNG contains transparent pixels",
                        "PNG width must be at least 2000 pixels",
                        "PNG height must be at least 600 pixels",
                        "temporary clipboard asset is published");
    }

    @Test
    void rejectsDuplicateIdsAssetsAndEscapingPaths() throws Exception {
        writeCatalog("""
                {
                  "schemaVersion": 2,
                  "diagrams": [
                    {
                      "id": "duplicate",
                      "sourceType": "CURATED_SVG",
                      "source": "docs/assets/shared.svg",
                      "svg": "docs/assets/shared.svg",
                      "png": "docs/assets/first.png",
                      "primaryOwner": "docs/first.md",
                      "consumers": []
                    },
                    {
                      "id": "duplicate",
                      "sourceType": "CURATED_SVG",
                      "source": "docs/assets/shared.svg",
                      "svg": "docs/assets/shared.svg",
                      "png": "../outside.png",
                      "primaryOwner": "docs/second.md",
                      "consumers": []
                    }
                  ]
                }
                """);
        write("docs/architecture/diagram-publication/mermaid-config.json", "{}\n");

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .extracting(DiagramPublicationValidator.Violation::reason)
                .contains("diagram id is duplicated",
                        "canonical SVG is assigned to more than one diagram",
                        "path escapes repository root");
    }

    @Test
    void enforcesCanonicalSourceTypeContracts() throws Exception {
        writeCatalog("""
                {
                  "schemaVersion": 2,
                  "diagrams": [{
                    "id": "split-source",
                    "sourceType": "CURATED_SVG",
                    "source": "docs/assets/source.svg",
                    "svg": "docs/assets/published.svg",
                    "png": "docs/assets/flow.png",
                    "primaryOwner": "docs/owner.md",
                    "consumers": []
                  }]
                }
                """);
        write("docs/architecture/diagram-publication/mermaid-config.json", "{}\n");

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .extracting(DiagramPublicationValidator.Violation::reason)
                .contains("curated SVG source must equal the published SVG");
    }

    @Test
    void requiresDirectionalConnectorsToDeclareTheirTarget() throws Exception {
        writeValidPublication("""
                <svg xmlns="http://www.w3.org/2000/svg"
                     viewBox="0 0 1200 800" role="img">
                  <title>Flow without a declared target</title>
                  <desc>The arrow target cannot be verified.</desc>
                  <defs>
                    <marker id="arrow"><path d="M0 0 L10 5 L0 10 Z"/></marker>
                    <style>.wire{marker-end:url(#arrow)}</style>
                  </defs>
                  <rect width="1200" height="800" fill="#FFFFFF"/>
                  <rect id="target" x="300" y="300" width="600" height="200"/>
                  <path class="wire" d="M600 100 V300"/>
                </svg>
                """);

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .extracting(DiagramPublicationValidator.Violation::reason)
                .contains("directional connector is missing data-target");
    }

    @Test
    void rejectsDirectionalConnectorThatMissesItsDeclaredTarget() throws Exception {
        writeValidPublication("""
                <svg xmlns="http://www.w3.org/2000/svg"
                     viewBox="0 0 1200 800" role="img">
                  <title>Flow with a floating arrow</title>
                  <desc>The arrow stops before reaching its declared target.</desc>
                  <defs>
                    <marker id="arrow"><path d="M0 0 L10 5 L0 10 Z"/></marker>
                    <style>.wire{marker-end:url(#arrow)}</style>
                  </defs>
                  <rect width="1200" height="800" fill="#FFFFFF"/>
                  <rect id="target" x="300" y="300" width="600" height="200"/>
                  <path class="wire" data-target="target" d="M600 100 V250"/>
                </svg>
                """);

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .extracting(DiagramPublicationValidator.Violation::reason)
                .contains("directional connector does not end on its declared target");
    }

    @Test
    void rejectsDirectionalConnectorThatRunsAlongItsTargetEdge() throws Exception {
        writeValidPublication("""
                <svg xmlns="http://www.w3.org/2000/svg"
                     viewBox="0 0 1200 800" role="img">
                  <title>Flow with a tangential arrow</title>
                  <desc>The arrow runs along the target border.</desc>
                  <defs>
                    <marker id="arrow"><path d="M0 0 L10 5 L0 10 Z"/></marker>
                    <style>.wire{marker-end:url(#arrow)}</style>
                  </defs>
                  <rect width="1200" height="800" fill="#FFFFFF"/>
                  <rect id="target" x="300" y="300" width="600" height="200"/>
                  <path class="wire" data-target="target" d="M100 300 H600"/>
                </svg>
                """);

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .extracting(DiagramPublicationValidator.Violation::reason)
                .contains("directional connector must enter its target perpendicularly");
    }

    private String singleEntry(String... consumers) {
        String consumerJson = java.util.Arrays.stream(consumers)
                .map(value -> "\"" + value + "\"")
                .collect(java.util.stream.Collectors.joining(", "));
        return """
                {
                  "schemaVersion": 2,
                  "diagrams": [{
                    "id": "flow",
                    "sourceType": "CURATED_SVG",
                    "source": "docs/assets/flow.svg",
                    "svg": "docs/assets/flow.svg",
                    "png": "docs/assets/flow.png",
                    "primaryOwner": "docs/owner.md",
                    "consumers": [%s]
                  }]
                }
                """.formatted(consumerJson);
    }

    private void writeCatalog(String json) throws Exception {
        write("docs/architecture/diagram-publication/diagram-publications.json", json);
    }

    private void writeValidPublication(String svg) throws Exception {
        writeCatalog(singleEntry());
        write("docs/owner.md", "![Flow](assets/flow.svg)\n");
        write("docs/assets/flow.svg", svg);
        write("docs/architecture/diagram-publication/mermaid-config.json", "{}\n");
        writePng("docs/assets/flow.png", 2000, 600, 0xFFFFFFFF);
    }

    private void write(String relativePath, String content) throws Exception {
        Path path = repository.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private void writePng(String relativePath, int width, int height, int argb)
            throws Exception {
        Path path = repository.resolve(relativePath);
        Files.createDirectories(path.getParent());
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }
        ImageIO.write(image, "png", path.toFile());
    }

    private static String safeSvg() {
        return """
                <svg xmlns="http://www.w3.org/2000/svg"
                     viewBox="0 0 1200 800" role="img">
                  <title>Safe diagram</title>
                  <desc>Fixture with an explicit opaque background.</desc>
                  <rect width="1200" height="800" fill="#FFFFFF"/>
                  <text x="80" y="100">Safe label</text>
                </svg>
                """;
    }
}

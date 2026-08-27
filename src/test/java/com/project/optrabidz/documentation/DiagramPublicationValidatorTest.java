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
    void reportsUnsafeIncompleteAndUnreadablePublicationAssets() throws Exception {
        writeInventory("""
                {
                  "schemaVersion": 1,
                  "renderer": {
                    "packageName": "@mermaid-js/mermaid-cli",
                    "version": "11.16.0",
                    "config": "docs/architecture/diagram-publication/mermaid-config.json"
                  },
                  "diagrams": [{
                    "id": "unsafe-flow",
                    "owner": "docs/owner.md",
                    "source": "docs/assets/unsafe-flow.mmd",
                    "sourceType": "MERMAID_FILE",
                    "githubSvg": "docs/assets/unsafe-flow.svg",
                    "jiraPng": "docs/assets/unsafe-flow.png",
                    "jiraPngRequired": true,
                    "remediation": "REGENERATE"
                  }]
                }
                """);
        write("docs/owner.md", "# Owner\n\nNo image is embedded.\n");
        write("docs/assets/unsafe-flow.mmd", "flowchart TB\nA --> B\n");
        write("docs/assets/unsafe-flow.svg", """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <style>.canvas { fill: rgba(0, 0, 0, 0); }</style>
                  <rect class="canvas" width="1200" height="800"/>
                  <script>window.alert('unsafe')</script>
                  <foreignObject><div>HTML label</div></foreignObject>
                </svg>
                """);
        writePng("docs/assets/unsafe-flow.png", 100, 100, 0x00FFFFFF);
        writePng("docs/assets/draft-clipboard-preview.png", 2400, 800, 0xFFFFFFFF);

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .extracting(DiagramPublicationValidator.Violation::reason)
                .contains(
                        "owner document does not embed the declared SVG",
                        "SVG is missing viewBox",
                        "SVG is missing title",
                        "SVG is missing description",
                        "SVG is missing an explicit background",
                        "SVG contains forbidden foreignObject",
                        "SVG contains forbidden script",
                        "PNG contains transparent pixels",
                        "PNG width must be at least 2000 pixels",
                        "PNG height must be at least 600 pixels",
                        "temporary clipboard asset is published"
                );
    }

    @Test
    void acceptsSafePublicationWithNonSiblingSourceAndOptionalJiraExport() throws Exception {
        writeInventory("""
                {
                  "schemaVersion": 1,
                  "renderer": {
                    "packageName": "@mermaid-js/mermaid-cli",
                    "version": "11.16.0",
                    "config": "docs/architecture/diagram-publication/mermaid-config.json"
                  },
                  "diagrams": [{
                    "id": "architecture-overview",
                    "owner": "docs/architecture/README.md",
                    "source": "docs/architecture/overview.mmd",
                    "sourceType": "MERMAID_FILE",
                    "githubSvg": "docs/architecture/assets/architecture-overview.svg",
                    "jiraPng": null,
                    "jiraPngRequired": false,
                    "remediation": "PASS"
                  }]
                }
                """);
        write("docs/architecture/README.md", """
                # Architecture

                <img src="assets/architecture-overview.svg" alt="Architecture overview">
                """);
        write("docs/architecture/overview.mmd", "flowchart TB\nA --> B\n");
        write("docs/architecture/assets/architecture-overview.svg", safeSvg());
        write("docs/architecture/diagram-publication/mermaid-config.json", "{}\n");

        assertThat(DiagramPublicationValidator.findViolations(repository)).isEmpty();
    }

    @Test
    void acceptsMarkdownImageAndOpaqueHighResolutionPng() throws Exception {
        writeInventory("""
                {
                  "schemaVersion": 1,
                  "renderer": {
                    "packageName": "@mermaid-js/mermaid-cli",
                    "version": "11.16.0",
                    "config": "docs/architecture/diagram-publication/mermaid-config.json"
                  },
                  "diagrams": [{
                    "id": "published-flow",
                    "owner": "docs/design.md",
                    "source": "docs/assets/published-flow.mmd",
                    "sourceType": "MERMAID_FILE",
                    "githubSvg": "docs/assets/published-flow.svg",
                    "jiraPng": "docs/assets/published-flow.png",
                    "jiraPngRequired": true,
                    "remediation": "REGENERATE"
                  }]
                }
                """);
        write("docs/design.md", "![Published flow](assets/published-flow.svg)\n");
        write("docs/assets/published-flow.mmd", "flowchart TB\nA --> B\n");
        write("docs/assets/published-flow.svg", safeSvg());
        write("docs/architecture/diagram-publication/mermaid-config.json", "{}\n");
        writePng("docs/assets/published-flow.png", 2000, 600, 0xFFFFFFFF);

        assertThat(DiagramPublicationValidator.findViolations(repository)).isEmpty();
    }

    @Test
    void acceptsOpaqueBackgroundDeclaredByAClassRule() throws Exception {
        writeInventory("""
                {
                  "schemaVersion": 1,
                  "renderer": {
                    "packageName": "@mermaid-js/mermaid-cli",
                    "version": "11.16.0",
                    "config": "docs/architecture/diagram-publication/mermaid-config.json"
                  },
                  "diagrams": [{
                    "id": "hand-authored-flow",
                    "owner": "docs/design.md",
                    "source": "docs/assets/hand-authored-flow.svg",
                    "sourceType": "HAND_AUTHORED_SVG",
                    "githubSvg": "docs/assets/hand-authored-flow.svg",
                    "jiraPng": null,
                    "jiraPngRequired": false,
                    "remediation": "PASS"
                  }]
                }
                """);
        write("docs/design.md",
                "![Hand-authored flow](assets/hand-authored-flow.svg)\n");
        write("docs/assets/hand-authored-flow.svg", """
                <svg xmlns="http://www.w3.org/2000/svg"
                     viewBox="0 0 1200 800" role="img">
                  <title>Hand-authored diagram</title>
                  <desc>Fixture with an opaque class-backed background.</desc>
                  <style>.canvas { fill: #F7F3EA; }</style>
                  <rect class="canvas" width="1200" height="800"/>
                </svg>
                """);
        write("docs/architecture/diagram-publication/mermaid-config.json", "{}\n");

        assertThat(DiagramPublicationValidator.findViolations(repository)).isEmpty();
    }

    @Test
    void rejectsInventoryPathsThatEscapeTheRepository() throws Exception {
        writeInventory("""
                {
                  "schemaVersion": 1,
                  "renderer": {
                    "packageName": "@mermaid-js/mermaid-cli",
                    "version": "11.16.0",
                    "config": "docs/architecture/diagram-publication/mermaid-config.json"
                  },
                  "diagrams": [{
                    "id": "escaping-flow",
                    "owner": "../outside.md",
                    "source": "docs/assets/flow.mmd",
                    "sourceType": "MERMAID_FILE",
                    "githubSvg": "docs/assets/flow.svg",
                    "jiraPng": null,
                    "jiraPngRequired": false,
                    "remediation": "REGENERATE"
                  }]
                }
                """);
        write("docs/architecture/diagram-publication/mermaid-config.json", "{}\n");

        assertThat(DiagramPublicationValidator.findViolations(repository))
                .extracting(DiagramPublicationValidator.Violation::reason)
                .contains("path escapes repository root");
    }

    private void writeInventory(String json) throws Exception {
        write("docs/architecture/diagram-publication/inventory.json", json);
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
                  <rect x="0" y="0" width="1200" height="800" fill="#FFFFFF"/>
                  <text x="80" y="100">Safe label</text>
                </svg>
                """;
    }
}

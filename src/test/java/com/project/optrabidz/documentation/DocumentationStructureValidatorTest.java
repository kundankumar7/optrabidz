package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentationStructureValidatorTest {

    @TempDir
    Path repository;

    @Test
    void reportsMissingEntriesAndReaderFacingSourceLeaks() throws Exception {
        write("README.md", """
                # Project

                [Internal plan](docs/api/work-items/KAN-1/implementation-plan.md)
                [Source](docs/api/assets/flow.mmd)

                ```mermaid
                flowchart TB
                A --> B
                ```
                """);

        assertThat(DocumentationStructureValidator.findViolations(repository))
                .extracting(DocumentationStructureValidator.Violation::reason)
                .contains(
                        "required documentation entry does not exist",
                        "reader-facing Markdown links Mermaid source",
                        "reader-facing Markdown contains a Mermaid fence",
                        "stable documentation links a work-item implementation plan");
    }

    @Test
    void acceptsStableTopicsAndIgnoresLinksInsideOrdinaryCodeFences() throws Exception {
        writeRequiredEntries();
        write("README.md", """
                # Project

                ![Architecture](docs/architecture/assets/overview.svg)
                [PNG fallback](docs/architecture/assets/overview.png)
                [Guide](docs/api/README.md)
                [External](https://example.test/reference)

                ```java
                String example = "[not-a-link](assets/source.mmd)";
                ```
                """);

        assertThat(DocumentationStructureValidator.findViolations(repository))
                .isEmpty();
    }

    @Test
    void limitsReaderFacingRulesToStableGuidance() throws Exception {
        writeRequiredEntries();
        write("docs/api/work-items/KAN-1/design.md", """
                # Historical design

                [Source](assets/flow.mmd)

                ```mermaid
                flowchart TB
                A --> B
                ```
                """);
        write("docs/database/assets/er-diagram-source.md", """
                # Diagram source

                ```mermaid
                erDiagram
                ACCOUNT ||--o{ SESSION : owns
                ```
                """);

        assertThat(DocumentationStructureValidator.findViolations(repository))
                .isEmpty();
    }

    @Test
    void requiresTheDocumentationMaintenanceMap() throws Exception {
        writeRequiredEntries();
        Files.delete(repository.resolve("docs/maintenance.md"));

        assertThat(DocumentationStructureValidator.findViolations(repository))
                .contains(new DocumentationStructureValidator.Violation(
                        "docs/maintenance.md",
                        "required documentation entry does not exist"));
    }

    private void writeRequiredEntries() throws Exception {
        write("docs/README.md", "# Documentation\n");
        write("docs/maintenance.md", "# Documentation maintenance\n");
        write("docs/getting-started/README.md", "# Getting started\n");
        write("docs/architecture/README.md", "# Architecture\n");
        write("docs/api/README.md", "# API\n");
        write("docs/database/README.md", "# Database\n");
        write("docs/security/README.md", "# Security\n");
        write("docs/operations/README.md", "# Operations\n");
        write("docs/decisions/README.md", "# Decisions\n");
    }

    private void write(String relativePath, String content) throws Exception {
        Path path = repository.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}

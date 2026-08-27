package com.project.optrabidz.documentation.error;

import com.project.optrabidz.common.api.error.ProblemTypeUri;
import com.project.optrabidz.common.error.ErrorCategory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCatalogueMarkdownSnapshotTest {

    private static final Path CATALOGUE = Path.of(
            "docs", "error-handling", "error-catalogue.md"
    );

    @Test
    void rendersTheStableHeaderAndPublicFields() {
        PublicErrorDefinition definition = definition(
                "LISTING_NOT_FOUND",
                "Resource not found",
                "The requested listing was not found"
        );

        String rendered = ErrorCatalogueMarkdownRenderer.render(
                List.of(definition)
        );

        assertThat(rendered).isEqualTo("""
                # Public Error Catalogue

                This file is generated from the application-owned public error definitions.
                Do not add diagnostic or secret values.

                | Code | Category | HTTP | Title | Safe detail | Type | Sources |
                |---|---|---:|---|---|---|---|
                | `LISTING_NOT_FOUND` | `NOT_FOUND` | 404 | Resource not found | The requested listing was not found | `urn:optrabidz:problem:listing-not-found` | `marketplace` |
                """);
    }

    @Test
    void escapesCharactersThatWouldBreakAMarkdownTableRow() {
        PublicErrorDefinition definition = definition(
                "LISTING_NOT_FOUND",
                "Resource | missing",
                "First\\part\r\nsecond | part"
        );

        String rendered = ErrorCatalogueMarkdownRenderer.render(
                List.of(definition)
        );

        assertThat(rendered).contains(
                "Resource \\| missing | First\\\\part\\r\\nsecond \\| part"
        );
    }

    @Test
    void checkedInCatalogueMatchesTheRuntimeDefinitions() throws Exception {
        String expected = ErrorCatalogueMarkdownRenderer.render(
                PublicErrorCatalogue.createDefault().entries()
        );
        if (Boolean.getBoolean("optrabidz.update-error-catalogue")) {
            Files.writeString(CATALOGUE, expected, StandardCharsets.UTF_8);
        }

        assertThat(Files.readString(CATALOGUE, StandardCharsets.UTF_8))
                .isEqualTo(expected);
    }

    private static PublicErrorDefinition definition(
            String code,
            String title,
            String detail
    ) {
        SortedSet<String> sources = new TreeSet<>();
        sources.add("marketplace");
        return new PublicErrorDefinition(
                code,
                404,
                title,
                detail,
                ProblemTypeUri.fromCode(code),
                Optional.of(ErrorCategory.NOT_FOUND),
                sources
        );
    }
}

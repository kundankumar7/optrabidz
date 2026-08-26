package com.project.optrabidz.documentation.error;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ErrorCatalogueMarkdownRenderer {

    private static final String HEADER = """
            # Public Error Catalogue

            This file is generated from the application-owned public error definitions.
            Do not add diagnostic or secret values.

            | Code | Category | HTTP | Title | Safe detail | Type | Sources |
            |---|---|---:|---|---|---|---|
            """;

    private ErrorCatalogueMarkdownRenderer() {
    }

    public static String render(List<PublicErrorDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions must not be null");
        StringBuilder markdown = new StringBuilder(HEADER);
        definitions.stream()
                .map(definition -> Objects.requireNonNull(
                        definition,
                        "definition must not be null"
                ))
                .sorted(Comparator.comparing(PublicErrorDefinition::code))
                .forEach(definition -> append(markdown, definition));
        return markdown.toString();
    }

    private static void append(
            StringBuilder markdown,
            PublicErrorDefinition definition
    ) {
        markdown.append("| `")
                .append(escape(definition.code()))
                .append("` | `")
                .append(definition.category().map(Enum::name).orElse("TRANSPORT"))
                .append("` | ")
                .append(definition.status())
                .append(" | ")
                .append(escape(definition.title()))
                .append(" | ")
                .append(escape(definition.detail()))
                .append(" | `")
                .append(escape(definition.type().toString()))
                .append("` | `")
                .append(escape(String.join(", ", definition.sources())))
                .append("` |\n");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}

package com.project.optrabidz.documentation.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentationExposureValidatorTest {

    private static final String API_DOCS_PATH = "/v3/api-docs";
    private static final String SWAGGER_UI_PATH = "/swagger-ui.html";

    @Test
    void acceptsTheApprovedEnvironmentMatrix() {
        assertValid("base", false, false, false, access("DISABLED"));
        assertValid("dev", true, true, false, access("PUBLIC"));
        assertValid("test", true, false, false, access("PUBLIC"));
        assertValid("prod", false, false, false, access("AUTHENTICATED"));
        assertValid("prod", true, false, false, access("AUTHENTICATED"));
    }

    @Test
    void rejectsEnabledJsonWithDisabledAccess() {
        assertInvalid(
                "any", true, false, false, access("DISABLED"),
                "api-docs-enabled", "access"
        );
    }

    @Test
    void rejectsUiWithoutJson() {
        assertInvalid(
                "any", false, true, false, access("PUBLIC"),
                "swagger-ui-enabled", "api-docs-enabled"
        );
    }

    @Test
    void rejectsAuthenticatedUi() {
        assertInvalid(
                "any", true, true, false, access("AUTHENTICATED"),
                "swagger-ui-enabled", "access"
        );
    }

    @Test
    void rejectsPublicDocumentationInProduction() {
        assertInvalid(
                "prod", true, true, false, access("PUBLIC"),
                "prod", "swagger-ui-enabled", "access"
        );
    }

    @Test
    void rejectsManagementPortPublication() {
        assertInvalid(
                "any", true, false, true, access("AUTHENTICATED"),
                "management-port-enabled"
        );
    }

    @Test
    void rejectsADirectSpringdocApiPathOverride() {
        assertThatThrownBy(() -> validator(
                properties(true, false, false, access("PUBLIC")),
                "test",
                "/internal/openapi",
                SWAGGER_UI_PATH,
                false
        ).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("springdoc.api-docs.path");
    }

    @Test
    void rejectsADirectSpringdocUiPathOverride() {
        assertThatThrownBy(() -> validator(
                properties(true, true, false, access("PUBLIC")),
                "dev",
                API_DOCS_PATH,
                "/internal/swagger",
                false
        ).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("springdoc.swagger-ui.path");
    }

    @Test
    void rejectsADirectSpringdocManagementPortOverride() {
        assertThatThrownBy(() -> validator(
                properties(true, false, false, access("AUTHENTICATED")),
                "prod",
                API_DOCS_PATH,
                SWAGGER_UI_PATH,
                true
        ).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("springdoc.use-management-port");
    }

    private static DocumentationExposureProperties.Access access(String name) {
        return DocumentationExposureProperties.Access.valueOf(name);
    }

    private static void assertValid(
            String profile,
            boolean apiDocs,
            boolean ui,
            boolean managementPort,
            DocumentationExposureProperties.Access access
    ) {
        DocumentationExposureProperties properties = properties(
                apiDocs, ui, managementPort, access
        );
        assertThatCode(() -> validator(properties, profile).validate())
                .doesNotThrowAnyException();
    }

    private static void assertInvalid(
            String profile,
            boolean apiDocs,
            boolean ui,
            boolean managementPort,
            DocumentationExposureProperties.Access access,
            String... messageFragments
    ) {
        DocumentationExposureProperties properties = properties(
                apiDocs, ui, managementPort, access
        );
        assertThatThrownBy(() -> validator(properties, profile).validate())
                .isInstanceOf(IllegalStateException.class)
                .satisfies(error -> {
                    for (String fragment : messageFragments) {
                        org.assertj.core.api.Assertions.assertThat(error)
                                .hasMessageContaining(fragment);
                    }
                });
    }

    private static DocumentationExposureValidator validator(
            DocumentationExposureProperties properties,
            String profile
    ) {
        return validator(
                properties,
                profile,
                API_DOCS_PATH,
                SWAGGER_UI_PATH,
                false
        );
    }

    private static DocumentationExposureValidator validator(
            DocumentationExposureProperties properties,
            String profile,
            String apiDocsPath,
            String swaggerUiPath,
            boolean springdocManagementPort
    ) {
        return new DocumentationExposureValidator(
                properties,
                Set.of(profile),
                apiDocsPath,
                swaggerUiPath,
                springdocManagementPort
        );
    }

    private static DocumentationExposureProperties properties(
            boolean apiDocs,
            boolean ui,
            boolean managementPort,
            DocumentationExposureProperties.Access access
    ) {
        return new DocumentationExposureProperties(
                apiDocs,
                ui,
                managementPort,
                access
        );
    }
}

package com.project.optrabidz.documentation.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentationExposureValidatorTest {

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
        return new DocumentationExposureValidator(
                properties,
                Set.of(profile)
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

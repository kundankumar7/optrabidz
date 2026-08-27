package com.project.optrabidz.documentation.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentationSecurityConfigurationTest {

    private final DocumentationSecurityConfiguration configuration =
            new DocumentationSecurityConfiguration();
    private final DocumentationExposureProperties publicDocumentation =
            new DocumentationExposureProperties(
                    true,
                    false,
                    false,
                    DocumentationExposureProperties.Access.PUBLIC
            );

    @Test
    void rejectsAnEffectiveApiDocsPathOverride() {
        assertInvalid(
                "springdoc.api-docs.path",
                "/internal/openapi",
                "springdoc.api-docs.path"
        );
    }

    @Test
    void rejectsAnEffectiveSwaggerUiPathOverride() {
        assertInvalid(
                "springdoc.swagger-ui.path",
                "/internal/swagger",
                "springdoc.swagger-ui.path"
        );
    }

    @Test
    void rejectsAnEffectiveManagementPortOverride() {
        assertInvalid(
                "springdoc.use-management-port",
                "true",
                "springdoc.use-management-port"
        );
    }

    private void assertInvalid(
            String property,
            String value,
            String expectedMessage
    ) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(property, value);

        assertThatThrownBy(() -> configuration
                .documentationExposureValidator(
                        publicDocumentation,
                        environment
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedMessage);
    }
}

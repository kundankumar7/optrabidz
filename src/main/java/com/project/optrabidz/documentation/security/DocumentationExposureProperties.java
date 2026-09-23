package com.project.optrabidz.documentation.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Controls whether API documentation is exposed and who may access it.
 *
 * @param apiDocsEnabled whether the OpenAPI document endpoint is enabled
 * @param swaggerUiEnabled whether the Swagger UI endpoint is enabled
 * @param managementPortEnabled whether documentation may be exposed on the management port
 * @param access access policy applied to enabled documentation endpoints
 */
@ConfigurationProperties(prefix = "optrabidz.documentation")
public record DocumentationExposureProperties(
        boolean apiDocsEnabled,
        boolean swaggerUiEnabled,
        boolean managementPortEnabled,
        Access access
) {

    public DocumentationExposureProperties {
        access = Objects.requireNonNull(access, "access must not be null");
    }

    public enum Access {
        DISABLED,
        PUBLIC,
        AUTHENTICATED
    }
}

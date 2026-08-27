package com.project.optrabidz.documentation.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

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

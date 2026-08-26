package com.project.optrabidz.documentation.security;

import java.util.Objects;
import java.util.Set;

public final class DocumentationExposureValidator {

    private final DocumentationExposureProperties properties;
    private final Set<String> activeProfiles;

    public DocumentationExposureValidator(
            DocumentationExposureProperties properties,
            Set<String> activeProfiles
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "properties must not be null"
        );
        this.activeProfiles = Set.copyOf(Objects.requireNonNull(
                activeProfiles,
                "activeProfiles must not be null"
        ));
    }

    public void validate() {
        if (properties.managementPortEnabled()) {
            reject("management-port-enabled must remain false");
        }
        if (properties.apiDocsEnabled()
                && properties.access()
                == DocumentationExposureProperties.Access.DISABLED) {
            reject("api-docs-enabled=true contradicts access=DISABLED");
        }
        if (properties.swaggerUiEnabled()
                && !properties.apiDocsEnabled()) {
            reject("swagger-ui-enabled=true requires api-docs-enabled=true");
        }
        if (properties.swaggerUiEnabled()
                && properties.access()
                != DocumentationExposureProperties.Access.PUBLIC) {
            reject("swagger-ui-enabled=true requires access=PUBLIC");
        }
        if (activeProfiles.contains("prod")
                && (properties.swaggerUiEnabled()
                || properties.access()
                == DocumentationExposureProperties.Access.PUBLIC)) {
            reject(
                    "prod forbids swagger-ui-enabled=true and access=PUBLIC"
            );
        }
    }

    private static void reject(String message) {
        throw new IllegalStateException(
                "Invalid documentation exposure configuration: " + message
        );
    }
}

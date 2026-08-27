package com.project.optrabidz.documentation.security;

import java.util.Objects;
import java.util.Set;

public final class DocumentationExposureValidator {

    static final String API_DOCS_PATH = "/v3/api-docs";
    static final String SWAGGER_UI_PATH = "/swagger-ui.html";

    private final DocumentationExposureProperties properties;
    private final Set<String> activeProfiles;
    private final String apiDocsPath;
    private final String swaggerUiPath;
    private final boolean springdocManagementPort;

    public DocumentationExposureValidator(
            DocumentationExposureProperties properties,
            Set<String> activeProfiles,
            String apiDocsPath,
            String swaggerUiPath,
            boolean springdocManagementPort
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "properties must not be null"
        );
        this.activeProfiles = Set.copyOf(Objects.requireNonNull(
                activeProfiles,
                "activeProfiles must not be null"
        ));
        this.apiDocsPath = Objects.requireNonNull(
                apiDocsPath,
                "apiDocsPath must not be null"
        );
        this.swaggerUiPath = Objects.requireNonNull(
                swaggerUiPath,
                "swaggerUiPath must not be null"
        );
        this.springdocManagementPort = springdocManagementPort;
    }

    public void validate() {
        if (!API_DOCS_PATH.equals(apiDocsPath)) {
            reject("springdoc.api-docs.path must remain " + API_DOCS_PATH);
        }
        if (!SWAGGER_UI_PATH.equals(swaggerUiPath)) {
            reject(
                    "springdoc.swagger-ui.path must remain "
                            + SWAGGER_UI_PATH
            );
        }
        if (springdocManagementPort) {
            reject("springdoc.use-management-port must remain false");
        }
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

package com.project.optrabidz.documentation.security;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentationExposureIT extends ApiIntegrationTestSupport {

    private static final List<String> API_DOC_PATHS = List.of(
            "/v3/api-docs",
            "/v3/api-docs/swagger-config",
            "/v3/api-docs.yaml"
    );
    private static final List<String> UI_PATHS = List.of(
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/webjars/swagger-ui/5.17.14/swagger-ui.css"
    );

    @Nested
    class TestProfileDefaults {

        @Autowired
        private MockMvc contextMockMvc;

        @Test
        void exposesJsonButDoesNotServeAnyUiSurface() throws Exception {
            contextMockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON
                    ));

            for (String path : UI_PATHS) {
                contextMockMvc.perform(get(path))
                        .andExpect(status().isUnauthorized())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_PROBLEM_JSON
                        ))
                        .andExpect(jsonPath("$.code").value(
                                "AUTHENTICATION_REQUIRED"
                        ));
            }
        }
    }

    @Nested
    @TestPropertySource(properties = {
            "optrabidz.documentation.api-docs-enabled=false",
            "optrabidz.documentation.swagger-ui-enabled=false",
            "optrabidz.documentation.management-port-enabled=false",
            "optrabidz.documentation.access=DISABLED"
    })
    class DisabledOverride {

        @Autowired
        private DocumentationExposureProperties exposure;

        @Autowired
        private MockMvc contextMockMvc;

        @Test
        void deniesEveryDocumentationSurfaceForAnonymousAndAuthenticatedCallers()
                throws Exception {
            assertThat(exposure.apiDocsEnabled()).isFalse();
            assertThat(exposure.swaggerUiEnabled()).isFalse();
            assertThat(exposure.access())
                    .isEqualTo(DocumentationExposureProperties.Access.DISABLED);
            AuthenticatedClient client = registerAndLogin(RoleType.STARTUP);
            for (String path : allDocumentationPaths()) {
                contextMockMvc.perform(get(path))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(
                                "AUTHENTICATION_REQUIRED"
                        ));
                contextMockMvc.perform(get(path)
                                .session(client.session())
                                .cookie(client.xsrfCookie()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(
                                "AUTHORIZATION_FAILED"
                        ));
            }
        }
    }

    @Nested
    @TestPropertySource(properties = {
            "optrabidz.documentation.api-docs-enabled=true",
            "optrabidz.documentation.swagger-ui-enabled=false",
            "optrabidz.documentation.management-port-enabled=false",
            "optrabidz.documentation.access=AUTHENTICATED"
    })
    class AuthenticatedOverride {

        @Autowired
        private DocumentationExposureProperties exposure;

        @Autowired
        private MockMvc contextMockMvc;

        @Test
        void requiresAValidApplicationSessionForJsonDocumentation()
                throws Exception {
            assertThat(exposure.apiDocsEnabled()).isTrue();
            assertThat(exposure.swaggerUiEnabled()).isFalse();
            assertThat(exposure.access()).isEqualTo(
                    DocumentationExposureProperties.Access.AUTHENTICATED
            );
            contextMockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(
                            MediaType.APPLICATION_PROBLEM_JSON
                    ))
                    .andExpect(jsonPath("$.code").value(
                            "AUTHENTICATION_REQUIRED"
                    ));

            AuthenticatedClient client = registerAndLogin(RoleType.STARTUP);
            contextMockMvc.perform(get("/v3/api-docs")
                            .session(client.session())
                            .cookie(client.xsrfCookie()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON
                    ));

            for (String path : UI_PATHS) {
                contextMockMvc.perform(get(path)
                                .session(client.session())
                                .cookie(client.xsrfCookie()))
                        .andExpect(result -> assertThat(
                                result.getResponse().getStatus()
                        ).isEqualTo(403));
            }
        }
    }

    private static List<String> allDocumentationPaths() {
        return java.util.stream.Stream.concat(
                API_DOC_PATHS.stream(),
                UI_PATHS.stream()
        ).toList();
    }
}

package com.project.optrabidz.security.api;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(DefaultDenySecurityIT.UnclassifiedRouteConfiguration.class)
class DefaultDenySecurityIT extends ApiIntegrationTestSupport {
    private static final String UNCLASSIFIED_PATH =
            "/api/v1/security-boundary-probe";

    @Test
    void anonymousCallerCannotAccessUnclassifiedRoute() throws Exception {
        mockMvc.perform(get(UNCLASSIFIED_PATH)
                        .header("X-Request-Id", "anonymous-default-deny-request"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.requestId").value(
                        "anonymous-default-deny-request"));
    }

    @Test
    void authenticatedCallerCannotAccessUnclassifiedRoute() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);

        mockMvc.perform(get(UNCLASSIFIED_PATH)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-Request-Id",
                                "authenticated-default-deny-request"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.requestId").value(
                        "authenticated-default-deny-request"));
    }

    @Test
    void anonymousErrorDispatchCannotBypassDefaultDeny() throws Exception {
        mockMvc.perform(get(UNCLASSIFIED_PATH)
                        .with(request -> {
                            request.setDispatcherType(DispatcherType.ERROR);
                            return request;
                        })
                        .header("X-Request-Id",
                                "anonymous-error-dispatch-request"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(
                        "AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.requestId").value(
                        "anonymous-error-dispatch-request"));
    }

    @Test
    void authenticatedErrorDispatchCannotBypassDefaultDeny()
            throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);

        mockMvc.perform(get(UNCLASSIFIED_PATH)
                        .with(request -> {
                            request.setDispatcherType(DispatcherType.ERROR);
                            return request;
                        })
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-Request-Id",
                                "authenticated-error-dispatch-request"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(
                        "AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.requestId").value(
                        "authenticated-error-dispatch-request"));
    }

    @Test
    void anonymousDirectErrorRequestIsDenied() throws Exception {
        mockMvc.perform(get("/error")
                        .header("X-Request-Id",
                                "anonymous-direct-error-request"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(
                        "AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.requestId").value(
                        "anonymous-direct-error-request"));
    }

    @Test
    void authenticatedDirectErrorRequestIsDenied() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);

        mockMvc.perform(get("/error")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-Request-Id",
                                "authenticated-direct-error-request"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(
                        "AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.requestId").value(
                        "authenticated-direct-error-request"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class UnclassifiedRouteConfiguration {
        @Bean
        RouterFunction<ServerResponse> unclassifiedRoute() {
            return RouterFunctions.route()
                    .GET(UNCLASSIFIED_PATH,
                            request -> ServerResponse.noContent().build())
                    .build();
        }
    }
}

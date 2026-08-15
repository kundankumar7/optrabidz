package com.project.optrabidz.security.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityApiIT extends ApiIntegrationTestSupport {
    private static final String INITIAL_PASSWORD = "Password01";
    private static final String CHANGED_PASSWORD = "Changed01";
    private static final String REQUEST_ID = "security-request-123";

    @Test
    void registerLoginAndMeUseStatefulSessionWithCsrfCookie() throws Exception {
        String email = uniqueEmail("startup-auth");

        register(email, INITIAL_PASSWORD, RoleType.STARTUP)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Account created successfully"));

        AuthenticatedClient client = login(email, INITIAL_PASSWORD);

        mockMvc.perform(get("/api/v1/me")
                        .session(client.session())
                        .cookie(client.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("STARTUP"))
                .andExpect(jsonPath("$.data.accountState").value("ACTIVE"))
                .andExpect(jsonPath("$.data.profileStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.data.actorType").value("STARTUP"))
                .andExpect(jsonPath("$.data.actorExists").value(false));
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        String secret = "secret-bearer-token";

        MvcResult result = expectSecurityProblem(
                mockMvc.perform(get("/api/v1/me")
                        .header("X-Request-Id", REQUEST_ID)
                        .header("Authorization", "Bearer " + secret)),
                401,
                "authentication-required",
                "Authentication required",
                "Authentication is required",
                "AUTHENTICATION_REQUIRED"
        ).andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(secret)
                .doesNotContain("Authorization");
    }

    @Test
    void unsafeRequestIdIsReplacedConsistentlyForSecurityFailure()
            throws Exception {
        String unsafeRequestId = "invalid request id!";

        MvcResult result = mockMvc.perform(get("/api/v1/me")
                        .header("X-Request-Id", unsafeRequestId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code").value(
                        "AUTHENTICATION_REQUIRED"
                ))
                .andReturn();

        String replacement = result.getResponse().getHeader("X-Request-Id");
        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsByteArray()
        );
        assertThat(replacement)
                .isNotBlank()
                .isNotEqualTo(unsafeRequestId)
                .matches("[A-Za-z0-9._-]+");
        assertThat(body.get("requestId").asText()).isEqualTo(replacement);
        assertThat(body.get("instance").asText()).isEqualTo(
                "urn:optrabidz:request:" + replacement
        );
    }

    @Test
    void mutatingProtectedEndpointRequiresMatchingCsrfHeader() throws Exception {
        AuthenticatedClient client = registerAndLogin(RoleType.STARTUP);

        MvcResult result = expectSecurityProblem(
                mockMvc.perform(post("/api/v1/auth/logout")
                        .session(client.session())
                        .cookie(client.xsrfCookie())
                        .header("X-Request-Id", REQUEST_ID)),
                403,
                "csrf-validation-failed",
                "Request security validation failed",
                "Request security validation failed",
                "CSRF_VALIDATION_FAILED"
        ).andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(client.csrfToken());

        String wrongToken = "wrong-csrf-token";
        MvcResult wrongTokenResult = expectSecurityProblem(
                mockMvc.perform(post("/api/v1/auth/logout")
                        .session(client.session())
                        .cookie(client.xsrfCookie())
                        .header("X-CSRF-TOKEN", wrongToken)
                        .header("X-Request-Id", REQUEST_ID)),
                403,
                "csrf-validation-failed",
                "Request security validation failed",
                "Request security validation failed",
                "CSRF_VALIDATION_FAILED"
        ).andReturn();

        assertThat(wrongTokenResult.getResponse().getContentAsString())
                .doesNotContain(wrongToken)
                .doesNotContain(client.csrfToken());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(client.session())
                        .cookie(client.xsrfCookie())
                        .header("X-CSRF-TOKEN", client.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Logged out successfully"));
    }

    @Test
    void changePasswordAcceptsCurrentPasswordAndRejectsOldPasswordAfterward() throws Exception {
        String email = uniqueEmail("startup-password");
        register(email, INITIAL_PASSWORD, RoleType.STARTUP)
                .andExpect(status().isCreated());
        AuthenticatedClient client = login(email, INITIAL_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .session(client.session())
                        .cookie(client.xsrfCookie())
                        .header("X-CSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "currentPassword", INITIAL_PASSWORD,
                                "newPassword", CHANGED_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Password updated successfully"));

        loginAttempt(email, INITIAL_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));

        loginAttempt(email, CHANGED_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void selfRegistrationRejectsAdminRole() throws Exception {
        register(uniqueEmail("admin-denied"), INITIAL_PASSWORD, RoleType.ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Only STARTUP or INVESTOR accounts can self-register"));
    }

    @Test
    void roleSpecificEndpointsRejectValidSessionWithWrongRole() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);

        expectSecurityProblem(
                mockMvc.perform(get("/api/v1/investor-preferences/me")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-Request-Id", REQUEST_ID)),
                403,
                "authorization-failed",
                "Access denied",
                "You are not authorized to perform this action",
                "AUTHORIZATION_FAILED"
        );

        expectSecurityProblem(
                mockMvc.perform(get("/api/v1/startup-classifications/me")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-Request-Id", REQUEST_ID)),
                403,
                "authorization-failed",
                "Access denied",
                "You are not authorized to perform this action",
                "AUTHORIZATION_FAILED"
        );
    }

    @Test
    void providerWebhookEndpointDoesNotRequireBrowserSessionOrCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/payment-providers/upi/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "PAYMENT_CONFIRMED",
                                  "paymentAttemptId": 1001,
                                  "providerPaymentId": "UPI-PAYMENT-1001",
                                  "providerEventId": "evt_1001"
                }
                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("Webhook signature is missing"));
    }

    private ResultActions expectSecurityProblem(
            ResultActions result,
            int expectedStatus,
            String typeSlug,
            String title,
            String detail,
            String code
    ) throws Exception {
        return result
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(header().string("X-Request-Id", REQUEST_ID))
                .andExpect(jsonPath("$.type").value(
                        "urn:optrabidz:problem:" + typeSlug
                ))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value(
                        "urn:optrabidz:request:" + REQUEST_ID
                ))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.violations").doesNotExist())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}

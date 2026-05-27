package com.project.optrabidz.security.api;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityApiIT extends ApiIntegrationTestSupport {
    private static final String INITIAL_PASSWORD = "Password01";
    private static final String CHANGED_PASSWORD = "Changed01";

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
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.message").value("Authentication is required"));
    }

    @Test
    void mutatingProtectedEndpointRequiresMatchingCsrfHeader() throws Exception {
        AuthenticatedClient client = registerAndLogin(RoleType.STARTUP);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(client.session())
                        .cookie(client.xsrfCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("CSRF validation failed"));

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

        mockMvc.perform(get("/api/v1/investor-preferences/me")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("You are not authorized to perform this action"));

        mockMvc.perform(get("/api/v1/startup-classifications/me")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("You are not authorized to perform this action"));
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
}

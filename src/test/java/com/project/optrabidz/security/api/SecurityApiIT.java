package com.project.optrabidz.security.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.List;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        loginAttempt(email, CHANGED_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void selfRegistrationRejectsAdminRole() throws Exception {
        expectApplicationProblem(
                register(uniqueEmail("admin-denied"), INITIAL_PASSWORD, RoleType.ADMIN),
                422,
                "SELF_REGISTRATION_NOT_ALLOWED",
                "Business rule violation",
                "Only startup or investor accounts can self-register"
        );
    }

    @Test
    void loginRejectionsShareOneDisclosureSafeProblemContract() throws Exception {
        String unknownEmail = uniqueEmail("unknown-login");
        String wrongPasswordEmail = registeredEmail("wrong-secret");
        String lockedEmail = registeredEmail("locked-login");
        String disabledEmail = registeredEmail("disabled-login");
        String suspendedEmail = registeredEmail("suspended-login");
        String deactivatedEmail = registeredEmail("deactivated-login");

        jdbcTemplate.update(
                "update credential set credential_status = 'LOCKED' where email = ?",
                lockedEmail);
        jdbcTemplate.update(
                "update credential set credential_status = 'DISABLED' where email = ?",
                disabledEmail);
        jdbcTemplate.update("""
                update account
                set account_state = 'SUSPENDED'
                where account_id = (select account_id from credential where email = ?)
                """, suspendedEmail);
        jdbcTemplate.update("""
                update account
                set account_state = 'DEACTIVATED', deactivated_at = now()
                where account_id = (select account_id from credential where email = ?)
                """, deactivatedEmail);

        List<MvcResult> results = List.of(
                rejectedLogin(unknownEmail, INITIAL_PASSWORD),
                rejectedLogin(wrongPasswordEmail, "WrongPassword01"),
                rejectedLogin(lockedEmail, INITIAL_PASSWORD),
                rejectedLogin(disabledEmail, INITIAL_PASSWORD),
                rejectedLogin(suspendedEmail, INITIAL_PASSWORD),
                rejectedLogin(deactivatedEmail, INITIAL_PASSWORD)
        );

        JsonNode expected = normalizeProblem(results.getFirst());
        for (MvcResult result : results) {
            assertThat(normalizeProblem(result)).isEqualTo(expected);
            assertThat(result.getResponse().getContentAsString())
                    .doesNotContain(unknownEmail)
                    .doesNotContain(wrongPasswordEmail)
                    .doesNotContain(lockedEmail)
                    .doesNotContain(disabledEmail)
                    .doesNotContain(suspendedEmail)
                    .doesNotContain(deactivatedEmail)
                    .doesNotContain("UNKNOWN_IDENTITY")
                    .doesNotContain("INVALID_SECRET")
                    .doesNotContain("CREDENTIAL_LOCKED")
                    .doesNotContain("CREDENTIAL_DISABLED")
                    .doesNotContain("ACCOUNT_RESTRICTED")
                    .doesNotContain("CredentialStatus")
                    .doesNotContain("InvalidCredentialsException");
        }
    }

    @Test
    void registrationAndPasswordFailuresUseModuleProblemContracts() throws Exception {
        String email = registeredEmail("security-contracts");

        expectApplicationProblem(
                register(email, INITIAL_PASSWORD, RoleType.STARTUP),
                409,
                "EMAIL_ALREADY_REGISTERED",
                "Request conflict",
                "Email is already registered"
        );
        expectApplicationProblem(
                register(uniqueEmail("weak-password"), "onlyletters", RoleType.STARTUP),
                400,
                "PASSWORD_POLICY_VIOLATION",
                "Request validation failed",
                "Password must contain at least one letter and one digit"
        );

        AuthenticatedClient client = login(email, INITIAL_PASSWORD);
        expectApplicationProblem(
                mockMvc.perform(post("/api/v1/auth/change-password")
                        .session(client.session())
                        .cookie(client.xsrfCookie())
                        .header("X-CSRF-TOKEN", client.csrfToken())
                        .header("X-Request-Id", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "currentPassword", "WrongPassword01",
                                "newPassword", CHANGED_PASSWORD
                        )))),
                401,
                "CURRENT_PASSWORD_INVALID",
                "Authentication required",
                "Current password is incorrect"
        );
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

    private ResultActions expectApplicationProblem(
            ResultActions result,
            int expectedStatus,
            String code,
            String title,
            String detail
    ) throws Exception {
        return result
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(
                        "urn:optrabidz:problem:" + code.toLowerCase().replace('_', '-')))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    private String registeredEmail(String prefix) throws Exception {
        String email = uniqueEmail(prefix);
        register(email, INITIAL_PASSWORD, RoleType.STARTUP)
                .andExpect(status().isCreated());
        return email;
    }

    private MvcResult rejectedLogin(String email, String password) throws Exception {
        return expectApplicationProblem(
                mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Request-Id", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password)))),
                401,
                "INVALID_CREDENTIALS",
                "Authentication required",
                "Invalid email or password"
        ).andReturn();
    }

    private JsonNode normalizeProblem(MvcResult result) throws Exception {
        ObjectNode body = (ObjectNode) objectMapper.readTree(
                result.getResponse().getContentAsByteArray());
        body.remove(List.of("requestId", "timestamp", "instance"));
        return body;
    }
}

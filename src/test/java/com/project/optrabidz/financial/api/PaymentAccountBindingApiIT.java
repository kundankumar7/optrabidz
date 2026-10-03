package com.project.optrabidz.financial.api;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentAccountBindingApiIT extends ApiIntegrationTestSupport {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void startupCanCreateVerifyReplaceReadAndDeactivateOwnedBinding() throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        AuthenticatedClient otherAccount = registerAndLogin(RoleType.INVESTOR);

        MvcResult created = createBinding(startup, "create-one")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerCode").value("DEMO"))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andReturn();
        var createdBody = objectMapper.readTree(created.getResponse().getContentAsString());
        long originalId = createdBody
                .get("paymentAccountBindingId").asLong();
        assertThat(created.getResponse().getHeader("Location"))
                .isEqualTo("/api/v1/payment-account-bindings/" + originalId);
        assertThat(createdBody.has("externalRecipientReference")).isFalse();
        assertThat(createdBody.has("idempotencyKey")).isFalse();
        assertThat(createdBody.get("maskedAccountLabel").asText())
                .matches("•••• [0-9a-f]{4}");

        createBinding(startup, "create-one")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentAccountBindingId").value(originalId));

        mockMvc.perform(get("/api/v1/payment-account-bindings/current")
                        .session(startup.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.binding.paymentAccountBindingId").value(originalId));

        bindingAction(startup, originalId, "verify")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.ready").value(true));

        bindingAction(startup, originalId, "verify")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_ACCOUNT_BINDING_STATE_CONFLICT"));

        MvcResult replaced = mockMvc.perform(post(
                                "/api/v1/payment-account-bindings/{bindingId}/actions/replace", originalId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotencyKey", "replace-one"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andReturn();
        long replacementId = objectMapper.readTree(replaced.getResponse().getContentAsString())
                .get("paymentAccountBindingId").asLong();
        assertThat(replacementId).isNotEqualTo(originalId);

        mockMvc.perform(get("/api/v1/payment-account-bindings/{bindingId}", originalId)
                        .session(startup.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEACTIVATED"));

        mockMvc.perform(get("/api/v1/payment-account-bindings/{bindingId}", originalId)
                        .session(otherAccount.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_ACCOUNT_BINDING_NOT_FOUND"));
        bindingAction(otherAccount, originalId, "verify")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_ACCOUNT_BINDING_NOT_FOUND"));
        bindingAction(otherAccount, originalId, "deactivate")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_ACCOUNT_BINDING_NOT_FOUND"));

        bindingAction(startup, replacementId, "deactivate")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEACTIVATED"));
        bindingAction(startup, replacementId, "deactivate")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_ACCOUNT_BINDING_STATE_CONFLICT"));
        bindingAction(startup, replacementId, "verify")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_ACCOUNT_BINDING_STATE_CONFLICT"));

        mockMvc.perform(get("/api/v1/payment-account-bindings/current")
                        .session(startup.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.binding").doesNotExist());
    }

    @Test
    void bindingEndpointsRequireAuthenticationAndValidateIdempotencyKey() throws Exception {
        mockMvc.perform(get("/api/v1/payment-account-bindings/current"))
                .andExpect(status().isUnauthorized());

        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);
        mockMvc.perform(post("/api/v1/payment-account-bindings")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotencyKey", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void administratorCannotUseParticipantBindingEndpoints() throws Exception {
        AuthenticatedClient administrator = administrator();

        mockMvc.perform(get("/api/v1/payment-account-bindings/current")
                        .session(administrator.session()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FINANCIAL_OPERATION_NOT_ALLOWED"));

        createBinding(administrator, "admin-command")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FINANCIAL_OPERATION_NOT_ALLOWED"));
    }

    @Test
    void replacingForeignBindingReturnsOwnerScopedNotFoundBeforeIdempotencyConflict() throws Exception {
        AuthenticatedClient owner = registerAndLogin(RoleType.STARTUP);
        AuthenticatedClient otherAccount = registerAndLogin(RoleType.INVESTOR);

        createBinding(owner, "owner-command")
                .andExpect(status().isCreated());
        MvcResult foreignBinding = createBinding(otherAccount, "foreign-command")
                .andExpect(status().isCreated())
                .andReturn();
        long foreignBindingId = objectMapper.readTree(
                        foreignBinding.getResponse().getContentAsString())
                .get("paymentAccountBindingId").asLong();

        mockMvc.perform(post(
                                "/api/v1/payment-account-bindings/{bindingId}/actions/replace",
                                foreignBindingId)
                        .session(owner.session())
                        .cookie(owner.xsrfCookie())
                        .header("X-CSRF-TOKEN", owner.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotencyKey", "owner-command"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_ACCOUNT_BINDING_NOT_FOUND"));
    }

    @Test
    void concurrentCreatesLeaveExactlyOneActiveBinding() throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> createConcurrently(
                    investor, "concurrent-one", ready, start));
            Future<Integer> second = executor.submit(() -> createConcurrently(
                    investor, "concurrent-two", ready, start));
            ready.await();
            start.countDown();

            assertThat(Set.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }

        mockMvc.perform(get("/api/v1/payment-account-bindings/current")
                        .session(investor.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.binding.paymentAccountBindingId").isNumber());
    }

    private org.springframework.test.web.servlet.ResultActions createBinding(
            AuthenticatedClient client,
            String idempotencyKey
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/payment-account-bindings")
                .session(client.session())
                .cookie(client.xsrfCookie())
                .header("X-CSRF-TOKEN", client.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("idempotencyKey", idempotencyKey))));
    }

    private org.springframework.test.web.servlet.ResultActions bindingAction(
            AuthenticatedClient client,
            long bindingId,
            String action
    ) throws Exception {
        return mockMvc.perform(post(
                        "/api/v1/payment-account-bindings/{bindingId}/actions/{action}", bindingId, action)
                .session(client.session())
                .cookie(client.xsrfCookie())
                .header("X-CSRF-TOKEN", client.csrfToken()));
    }

    private int createConcurrently(AuthenticatedClient client,
                                   String idempotencyKey,
                                   CountDownLatch ready,
                                   CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return createBinding(client, idempotencyKey)
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private AuthenticatedClient administrator() throws Exception {
        String email = uniqueEmail("binding-admin");
        register(email, DEFAULT_PASSWORD, RoleType.INVESTOR)
                .andExpect(status().isCreated());
        int updated = jdbcTemplate.update("""
                update role
                set role_type = 'ADMIN'
                where account_id = (
                    select account_id from credential where email = ?
                )
                """, email);
        assertThat(updated).isEqualTo(1);
        return login(email, DEFAULT_PASSWORD);
    }
}

package com.project.optrabidz.financial.api;

import com.project.optrabidz.common.outbox.OutboxDispatcher;
import com.project.optrabidz.financial.infrastructure.provider.demo.DemoPaymentProviderProperties;
import com.project.optrabidz.financial.infrastructure.provider.demo.DemoPayoutBehavior;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles(profiles = {"test", "demo"}, inheritProfiles = false)
@TestPropertySource(properties = {
        "optrabidz.financial.local-provider.enabled=false",
        "optrabidz.financial.sandbox-providers.enabled=false",
        "optrabidz.financial.demo-provider.enabled=true",
        "optrabidz.financial.webhook.providers.DEMO.enabled=true",
        "optrabidz.financial.webhook.providers.DEMO.active-secret=test-only-demo-webhook-secret-material-0001"
})
class DemoPaymentApiIT extends ApiIntegrationTestSupport {
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired OutboxDispatcher outboxDispatcher;
    @Autowired DemoPaymentProviderProperties demoPaymentProviderProperties;

    @Test
    void successfulCollectionCreatesDiscoverablePayoutAndFinalizesSettlement() throws Exception {
        String payerEmail = uniqueEmail("demo-settlement-payer");
        register(payerEmail, DEFAULT_PASSWORD, RoleType.INVESTOR)
                .andExpect(status().isCreated());
        AuthenticatedClient payer = login(payerEmail, DEFAULT_PASSWORD);
        AuthenticatedClient outsider = registerAndLogin(RoleType.STARTUP);
        Long payerAccountId = accountId(payerEmail);

        Instant now = Instant.now();
        PostgresTestDataFixture.PaymentReference reference =
                new PostgresTestDataFixture(jdbcTemplate, now)
                        .createSettlementReference("demo-payout-" + UUID.randomUUID());
        insertVerifiedBinding(reference.payeeAccountId(), now);
        long intentId = insertSettlementIntent(
                reference.referenceId(), payerAccountId, reference.payeeAccountId(), now);
        long attemptId = insertAttempt(intentId, now);

        submitOutcome(payer, attemptId,
                json(Map.of("outcome", "SUCCESS", "idempotencyKey", "payout-success")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentState").value("PAYMENT_CONFIRMED"));

        mockMvc.perform(get("/api/v1/payment-intents/{id}/timeline", intentId)
                        .session(payer.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demonstration").value(true))
                .andExpect(jsonPath("$.entries[5].stage")
                        .value("PAYOUT_WAITING_FOR_DESTINATION"));

        assertThat(outboxDispatcher.dispatchPending()).isPositive();

        MvcResult payoutResult = mockMvc.perform(get(
                                "/api/v1/payment-intents/{id}/payout-transfer", intentId)
                        .session(payer.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").value(intentId))
                .andExpect(jsonPath("$.providerCode").value("DEMO"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.amount").value(550000.00))
                .andExpect(jsonPath("$.currencyCode").value("INR"))
                .andExpect(jsonPath("$.maskedDestination").value("•••• 1234"))
                .andExpect(jsonPath("$.attemptCount").value(1))
                .andExpect(jsonPath("$.idempotencyKey").doesNotExist())
                .andExpect(jsonPath("$.providerTransferReference").doesNotExist())
                .andReturn();
        long payoutTransferId = objectMapper.readTree(
                payoutResult.getResponse().getContentAsString())
                .get("payoutTransferId").asLong();

        mockMvc.perform(get("/api/v1/payout-transfers/{id}", payoutTransferId)
                        .session(payer.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payoutTransferId").value(payoutTransferId));
        mockMvc.perform(get("/api/v1/payout-transfers/{id}", payoutTransferId)
                        .session(outsider.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYOUT_TRANSFER_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/payout-transfers/{id}/actions/retry", payoutTransferId)
                        .session(payer.session())
                        .cookie(payer.xsrfCookie())
                        .header("X-CSRF-TOKEN", payer.csrfToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYOUT_TRANSFER_STATE_CONFLICT"));

        mockMvc.perform(get("/api/v1/payment-intents/{id}/timeline", intentId)
                        .session(payer.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[?(@.stage == 'PAYOUT_CONFIRMED')]").exists())
                .andExpect(jsonPath("$.entries[?(@.stage == 'SETTLEMENT_CONFIRMED')]").exists());
        assertThat(jdbcTemplate.queryForObject(
                "select settlement_state::text from settlement where settlement_id = ?",
                String.class, reference.referenceId()))
                .isEqualTo("SETTLEMENT_CONFIRMED");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from repayment where agreement_id = (select agreement_id from settlement where settlement_id = ?)",
                Integer.class, reference.referenceId()))
                .isEqualTo(1);
    }

    @Test
    void failedPayoutRequiresAuthenticatedRetryAndDoesNotRecollect() throws Exception {
        demoPaymentProviderProperties.setPayoutBehavior(DemoPayoutBehavior.FAIL_FIRST_ATTEMPT);
        try {
            String payerEmail = uniqueEmail("demo-retry-payer");
            register(payerEmail, DEFAULT_PASSWORD, RoleType.INVESTOR)
                    .andExpect(status().isCreated());
            AuthenticatedClient payer = login(payerEmail, DEFAULT_PASSWORD);
            Long payerAccountId = accountId(payerEmail);
            Instant now = Instant.now();
            PostgresTestDataFixture.PaymentReference reference =
                    new PostgresTestDataFixture(jdbcTemplate, now)
                            .createSettlementReference("demo-retry-" + UUID.randomUUID());
            insertVerifiedBinding(reference.payeeAccountId(), now);
            long intentId = insertSettlementIntent(
                    reference.referenceId(), payerAccountId,
                    reference.payeeAccountId(), now);
            long attemptId = insertAttempt(intentId, now);

            submitOutcome(payer, attemptId,
                    json(Map.of("outcome", "SUCCESS", "idempotencyKey", "retry-flow")))
                    .andExpect(status().isOk());
            assertThat(outboxDispatcher.dispatchPending()).isPositive();

            MvcResult failedResult = mockMvc.perform(get(
                                    "/api/v1/payment-intents/{id}/payout-transfer", intentId)
                            .session(payer.session()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"))
                    .andExpect(jsonPath("$.attemptCount").value(1))
                    .andExpect(jsonPath("$.latestFailureCode").value("DEMO_PAYOUT_FAILED"))
                    .andReturn();
            long transferId = objectMapper.readTree(
                    failedResult.getResponse().getContentAsString())
                    .get("payoutTransferId").asLong();
            assertThat(jdbcTemplate.queryForObject(
                    "select settlement_state::text from settlement where settlement_id = ?",
                    String.class, reference.referenceId()))
                    .isEqualTo("SETTLEMENT_PAYOUT_PENDING");

            mockMvc.perform(post("/api/v1/payout-transfers/{id}/actions/retry", transferId)
                            .session(payer.session()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"));
            mockMvc.perform(post("/api/v1/payout-transfers/{id}/actions/retry", transferId)
                            .session(payer.session())
                            .cookie(payer.xsrfCookie())
                            .header("X-CSRF-TOKEN", payer.csrfToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.attemptCount").value(2));

            assertThat(jdbcTemplate.queryForObject(
                    "select settlement_state::text from settlement where settlement_id = ?",
                    String.class, reference.referenceId()))
                    .isEqualTo("SETTLEMENT_CONFIRMED");
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from payment_attempt where payment_intent_id = ?",
                    Integer.class, intentId)).isEqualTo(1);
        } finally {
            demoPaymentProviderProperties.setPayoutBehavior(DemoPayoutBehavior.SUCCESS);
        }
    }

    @Test
    void payoutAndBusinessFinalizationRollbackTogetherWhenFinalizationFails() throws Exception {
        String payerEmail = uniqueEmail("demo-rollback-payer");
        register(payerEmail, DEFAULT_PASSWORD, RoleType.INVESTOR)
                .andExpect(status().isCreated());
        AuthenticatedClient payer = login(payerEmail, DEFAULT_PASSWORD);
        Long payerAccountId = accountId(payerEmail);
        Instant now = Instant.now();
        PostgresTestDataFixture.PaymentReference reference =
                new PostgresTestDataFixture(jdbcTemplate, now)
                        .createSettlementReference("demo-rollback-" + UUID.randomUUID());
        insertVerifiedBinding(reference.payeeAccountId(), now);
        long intentId = insertSettlementIntent(
                reference.referenceId(), payerAccountId,
                reference.payeeAccountId(), now);
        long attemptId = insertAttempt(intentId, now);

        jdbcTemplate.update("""
                delete from agreement_debt_terms
                where agreement_id = (
                    select agreement_id from settlement where settlement_id = ?
                )
                """, reference.referenceId());

        submitOutcome(payer, attemptId,
                json(Map.of("outcome", "SUCCESS", "idempotencyKey", "rollback-flow")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentState").value("PAYMENT_CONFIRMED"));

        assertThat(outboxDispatcher.dispatchPending()).isPositive();

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from payout_transfer where payment_intent_id = ?",
                Integer.class, intentId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select settlement_state::text from settlement where settlement_id = ?",
                String.class, reference.referenceId()))
                .isEqualTo("SETTLEMENT_PAYOUT_PENDING");
        assertThat(jdbcTemplate.queryForObject("""
                select retry_count
                from event_outbox
                where event_type = 'PaymentCollectionConfirmedEvent'
                  and cast(payload ->> 'paymentIntentId' as bigint) = ?
                """, Integer.class, intentId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from event_outbox
                where event_type in (
                    'PayoutTransferCreatedEvent',
                    'PayoutTransferConfirmedEvent',
                    'SettlementConfirmedEvent'
                )
                  and payload ->> 'paymentIntentId' = ?
                """, Integer.class, Long.toString(intentId))).isZero();
    }

    @Test
    void demoCheckoutAndSignedSuccessEnforceOwnershipCsrfReplayAndCollision() throws Exception {
        String payerEmail = uniqueEmail("demo-payer");
        register(payerEmail, DEFAULT_PASSWORD, RoleType.STARTUP).andExpect(status().isCreated());
        AuthenticatedClient payer = login(payerEmail, DEFAULT_PASSWORD);
        AuthenticatedClient other = registerAndLogin(RoleType.INVESTOR);
        Long payerAccountId = accountId(payerEmail);

        Instant now = Instant.now();
        PostgresTestDataFixture.PaymentReference reference =
                new PostgresTestDataFixture(jdbcTemplate, now)
                        .createRepaymentInstallmentReference("demo-api-" + UUID.randomUUID());
        insertVerifiedBinding(reference.payeeAccountId(), now);
        jdbcTemplate.update("""
                update repayment_installment
                set installment_status = 'PAYMENT_IN_PROGRESS', payment_started_at = ?, updated_at = ?
                where repayment_installment_id = ?
                """, Timestamp.from(now.minusSeconds(30)), Timestamp.from(now.minusSeconds(30)),
                reference.referenceId());
        long intentId = insertRepaymentIntent(
                reference.referenceId(), payerAccountId, reference.payeeAccountId(), now);
        long attemptId = insertAttempt(intentId, now);

        mockMvc.perform(get("/api/v1/payment-attempts/{id}/demo-checkout", attemptId)
                        .session(other.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_ATTEMPT_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/payment-attempts/{id}/demo-checkout", attemptId)
                        .session(payer.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("DEMO"))
                .andExpect(jsonPath("$.environmentLabel").value("DEMONSTRATION"))
                .andExpect(jsonPath("$.warning").value(
                        "Demonstration payment environment. No real financial transaction will occur."))
                .andExpect(jsonPath("$.providerPayload").doesNotExist());

        String success = json(Map.of("outcome", "SUCCESS", "idempotencyKey", "same-key"));
        submitOutcome(other, attemptId, success)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_ATTEMPT_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/payment-attempts/{id}/actions/demo-outcome", attemptId)
                        .session(payer.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(success))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"));

        submitOutcome(payer, attemptId, success)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptState").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentState").value("PAYMENT_CONFIRMED"))
                .andExpect(jsonPath("$.processedAt").isString());
        jdbcTemplate.update("""
                update payment_account_binding
                set binding_status = 'DEACTIVATED', deactivated_at = ?
                where account_id = ? and provider_code = 'DEMO'
                """, Timestamp.from(Instant.now()), reference.payeeAccountId());
        submitOutcome(payer, attemptId, success)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptState").value("CONFIRMED"));
        mockMvc.perform(get("/api/v1/payment-attempts/{id}/demo-checkout", attemptId)
                        .session(payer.session()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_STATE_CONFLICT"));

        submitOutcome(payer, attemptId,
                json(Map.of("outcome", "FAILURE", "idempotencyKey", "same-key")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEMO_PAYMENT_OUTCOME_IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void cancellationPersistsCancelledCollectionWithoutRequiringReceivingBinding() throws Exception {
        String payerEmail = uniqueEmail("demo-cancel-payer");
        register(payerEmail, DEFAULT_PASSWORD, RoleType.INVESTOR).andExpect(status().isCreated());
        AuthenticatedClient payer = login(payerEmail, DEFAULT_PASSWORD);
        Long payerAccountId = accountId(payerEmail);
        Instant now = Instant.now();
        PostgresTestDataFixture.PaymentReference reference =
                new PostgresTestDataFixture(jdbcTemplate, now).createSettlementReference("demo-cancel-" + UUID.randomUUID());
        long intentId = insertSettlementIntent(reference.referenceId(), payerAccountId, reference.payeeAccountId(), now);
        long attemptId = insertAttempt(intentId, now);

        submitOutcome(payer, attemptId,
                json(Map.of("outcome", "CANCELLATION", "idempotencyKey", "cancel-key")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptState").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentState").value("PAYMENT_CANCELLED"));

        assertThat(jdbcTemplate.queryForObject(
                "select settlement_state::text from settlement where settlement_id = ?",
                String.class, reference.referenceId())).isEqualTo("SETTLEMENT_PENDING");
    }

    private org.springframework.test.web.servlet.ResultActions submitOutcome(
            AuthenticatedClient client, long attemptId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/payment-attempts/{id}/actions/demo-outcome", attemptId)
                .session(client.session())
                .cookie(client.xsrfCookie())
                .header("X-CSRF-TOKEN", client.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Long accountId(String email) {
        return jdbcTemplate.queryForObject(
                "select account_id from credential where email = ?", Long.class, email);
    }

    private void insertVerifiedBinding(Long accountId, Instant now) {
        jdbcTemplate.update("""
                insert into payment_account_binding (
                    account_id, provider_code, command_idempotency_key,
                    external_recipient_reference, binding_status,
                    masked_account_suffix, demo_bank_label, created_at, verified_at
                ) values (?, 'DEMO', ?, ?, 'VERIFIED', '1234', 'Demonstration Bank', ?, ?)
                """, accountId, "binding-" + UUID.randomUUID(), "recipient-" + UUID.randomUUID(),
                Timestamp.from(now.minusSeconds(60)), Timestamp.from(now.minusSeconds(30)));
    }

    private long insertSettlementIntent(Long settlementId,
                                        Long payerAccountId,
                                        Long payeeAccountId,
                                        Instant now) {
        return jdbcTemplate.queryForObject("""
                insert into payment_intent (
                    payment_purpose, settlement_id, payer_account_id, payee_account_id,
                    amount, currency_code, payment_state, idempotency_key, created_at, expires_at
                ) values ('SETTLEMENT', ?, ?, ?, 550000.00, 'INR', 'PAYMENT_PENDING', ?, ?, ?)
                returning payment_intent_id
                """, Long.class, settlementId, payerAccountId, payeeAccountId,
                "intent-" + UUID.randomUUID(), Timestamp.from(now.minusSeconds(30)),
                Timestamp.from(now.plusSeconds(600)));
    }

    private long insertRepaymentIntent(Long installmentId,
                                       Long payerAccountId,
                                       Long payeeAccountId,
                                       Instant now) {
        return jdbcTemplate.queryForObject("""
                insert into payment_intent (
                    payment_purpose, repayment_installment_id, payer_account_id, payee_account_id,
                    amount, currency_code, payment_state, idempotency_key, created_at, expires_at
                ) values ('REPAYMENT', ?, ?, ?, 550000.00, 'INR', 'PAYMENT_PENDING', ?, ?, ?)
                returning payment_intent_id
                """, Long.class, installmentId, payerAccountId, payeeAccountId,
                "intent-" + UUID.randomUUID(), Timestamp.from(now.minusSeconds(30)),
                Timestamp.from(now.plusSeconds(600)));
    }

    private long insertAttempt(Long intentId, Instant now) {
        return jdbcTemplate.queryForObject("""
                insert into payment_attempt (
                    payment_intent_id, provider_code, method_type, provider_order_id,
                    provider_reference_id, attempt_state, created_at, initiated_at, provider_payload
                ) values (?, 'DEMO', 'UPI', ?, ?, 'INITIATED', ?, ?, cast(? as jsonb))
                returning payment_attempt_id
                """, Long.class, intentId, "DEMO-ORDER-TEST-" + UUID.randomUUID(),
                "DEMO-CHECKOUT-TEST-" + UUID.randomUUID(), Timestamp.from(now.minusSeconds(20)),
                Timestamp.from(now.minusSeconds(10)), "{\"environment\":\"DEMONSTRATION\"}");
    }
}

package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.port.PaymentWebhookReplayStore;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayContent;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayEvent;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayState;
import com.project.optrabidz.financial.application.replay.StoredPaymentWebhookReplayEvent;
import com.project.optrabidz.testsupport.PostgresJpaIntegrationTestSupport;
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({
        PostgresPaymentWebhookReplayStore.class,
        JacksonAutoConfiguration.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentWebhookReplayStoreIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");
    private static final String EVENT_PREFIX = "kan32-store-";

    @Autowired
    private PaymentWebhookReplayStore store;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void removeReplayRows() {
        jdbcTemplate.update(
                "delete from payment_webhook_event where provider_event_id like ?",
                EVENT_PREFIX + "%"
        );
    }

    @Test
    void firstClaimPersistsOnlyAllowlistedNormalizedContent() {
        PaymentWebhookReplayEvent event = confirmedEvent(EVENT_PREFIX + "first");

        OptionalLong claim = store.tryClaim(event, NOW);

        assertThat(claim).isPresent();
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                select processing_state::text as state,
                       payment_intent_id,
                       payment_attempt_id,
                       payload_hash,
                       payload::text as payload,
                       failure_message
                from payment_webhook_event
                where payment_webhook_event_id = ?
                """, claim.getAsLong());
        assertThat(row.get("state")).isEqualTo("RECEIVED");
        assertThat(row.get("payment_intent_id")).isNull();
        assertThat(row.get("payment_attempt_id")).isNull();
        assertThat(row.get("payload_hash")).isEqualTo(event.payloadHash());
        assertThat(row.get("payload").toString())
                .contains("\"fingerprintVersion\": 1")
                .contains("\"providerCode\": \"RAZORPAY\"")
                .doesNotContain("signature", "rawBody", "headers", "secret");
        assertThat(row.get("failure_message")).isNull();
    }

    @Test
    void completedDuplicateReturnsEmptyAndLoadsTheCommittedEvent() {
        PaymentReference reference = createPaymentReference();
        PaymentWebhookReplayEvent event = confirmedEvent(EVENT_PREFIX + "duplicate");
        long id = store.tryClaim(event, NOW).orElseThrow();
        store.markProcessed(
                id,
                reference.paymentIntentId(),
                reference.paymentAttemptId(),
                NOW.plusSeconds(1)
        );

        assertThat(store.tryClaim(event, NOW.plusSeconds(2))).isEmpty();
        assertThat(store.findByIdentity("RAZORPAY", EVENT_PREFIX + "duplicate"))
                .contains(new StoredPaymentWebhookReplayEvent(
                        id,
                        PaymentWebhookReplayState.PROCESSED,
                        event
                ));
    }

    @Test
    void conflictingContentDoesNotReplaceTheStoredWinner() {
        PaymentWebhookReplayEvent winner = confirmedEvent(EVENT_PREFIX + "collision");
        PaymentWebhookReplayEvent changed = event(
                EVENT_PREFIX + "collision",
                "provider-payment-changed"
        );
        long id = store.tryClaim(winner, NOW).orElseThrow();

        assertThat(store.tryClaim(changed, NOW.plusSeconds(1))).isEmpty();
        assertThat(store.findByIdentity("RAZORPAY", EVENT_PREFIX + "collision"))
                .contains(new StoredPaymentWebhookReplayEvent(
                        id,
                        PaymentWebhookReplayState.RECEIVED,
                        winner
                ));
    }

    @Test
    void completionRejectsAnEventThatIsNotReceived() {
        PaymentReference reference = createPaymentReference();
        PaymentWebhookReplayEvent event = confirmedEvent(EVENT_PREFIX + "state");
        long id = store.tryClaim(event, NOW).orElseThrow();
        store.markProcessed(
                id,
                reference.paymentIntentId(),
                reference.paymentAttemptId(),
                NOW.plusSeconds(1)
        );

        assertThatThrownBy(() -> store.markProcessed(
                id,
                reference.paymentIntentId(),
                reference.paymentAttemptId(),
                NOW.plusSeconds(2)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment webhook replay completion invariant failed");
    }

    @Test
    void rollbackRemovesTheClaimSoTheSameIdentityCanBeClaimedAgain() {
        PaymentWebhookReplayEvent event = confirmedEvent(EVENT_PREFIX + "rollback");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            assertThat(store.tryClaim(event, NOW)).isPresent();
            status.setRollbackOnly();
        });

        assertThat(store.tryClaim(event, NOW.plusSeconds(1))).isPresent();
    }

    @Test
    void concurrentClaimsProduceOneOwnerAndOneCommittedDuplicate()
            throws Exception {
        PaymentWebhookReplayEvent event = confirmedEvent(EVENT_PREFIX + "concurrent");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<OptionalLong>> futures = List.of(
                    executor.submit(() -> claimAfterSignal(event, ready, start)),
                    executor.submit(() -> claimAfterSignal(event, ready, start))
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<OptionalLong> results = List.of(
                    futures.get(0).get(10, TimeUnit.SECONDS),
                    futures.get(1).get(10, TimeUnit.SECONDS)
            );

            assertThat(results.stream().filter(OptionalLong::isPresent)).hasSize(1);
            assertThat(results.stream().filter(OptionalLong::isEmpty)).hasSize(1);
            assertThat(jdbcTemplate.queryForObject("""
                    select count(*) from payment_webhook_event
                    where provider_code = 'RAZORPAY' and provider_event_id = ?
                    """, Long.class, EVENT_PREFIX + "concurrent")).isEqualTo(1L);
            assertThat(store.findByIdentity(
                    "RAZORPAY", EVENT_PREFIX + "concurrent"
            )).isPresent();
        } finally {
            executor.shutdownNow();
        }
    }

    private OptionalLong claimAfterSignal(
            PaymentWebhookReplayEvent event,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return new TransactionTemplate(transactionManager).execute(
                status -> store.tryClaim(event, NOW)
        );
    }

    private PaymentReference createPaymentReference() {
        PostgresTestDataFixture.PaymentReference reference =
                new PostgresTestDataFixture(jdbcTemplate, NOW)
                        .createSettlementReference("kan32-store-" + UUID.randomUUID());
        Long paymentIntentId = jdbcTemplate.queryForObject("""
                insert into payment_intent (
                    payment_purpose,
                    settlement_id,
                    repayment_installment_id,
                    payer_account_id,
                    payee_account_id,
                    amount,
                    currency_code,
                    payment_state,
                    idempotency_key,
                    created_at,
                    expires_at
                ) values (
                    'SETTLEMENT', ?, null, ?, ?, 550000.00, 'INR',
                    'PAYMENT_PENDING', ?, ?, ?
                ) returning payment_intent_id
                """, Long.class,
                reference.referenceId(),
                reference.payerAccountId(),
                reference.payeeAccountId(),
                "kan32-store-" + UUID.randomUUID(),
                Timestamp.from(NOW.minusSeconds(30)),
                Timestamp.from(NOW.plusSeconds(900))
        );
        Long paymentAttemptId = jdbcTemplate.queryForObject("""
                insert into payment_attempt (
                    payment_intent_id,
                    provider_code,
                    method_type,
                    provider_order_id,
                    attempt_state,
                    created_at,
                    initiated_at,
                    provider_payload
                ) values (
                    ?, 'RAZORPAY', 'UPI', ?, 'INITIATED', ?, ?, cast('{}' as jsonb)
                ) returning payment_attempt_id
                """, Long.class,
                paymentIntentId,
                "kan32-store-order-" + UUID.randomUUID(),
                Timestamp.from(NOW.minusSeconds(20)),
                Timestamp.from(NOW.minusSeconds(10))
        );
        return new PaymentReference(paymentIntentId, paymentAttemptId);
    }

    private static PaymentWebhookReplayEvent confirmedEvent(String providerEventId) {
        return event(providerEventId, "provider-payment-1001");
    }

    private static PaymentWebhookReplayEvent event(
            String providerEventId,
            String providerPaymentId) {
        PaymentWebhookReplayContent content = new PaymentWebhookReplayContent(
                1,
                "RAZORPAY",
                providerEventId,
                PaymentProviderWebhookEventType.PAYMENT_CONFIRMED,
                1001L,
                providerPaymentId,
                null,
                null
        );
        String payloadHash = providerPaymentId.equals("provider-payment-1001")
                ? "a".repeat(64)
                : "b".repeat(64);
        return new PaymentWebhookReplayEvent(content, payloadHash);
    }

    private record PaymentReference(long paymentIntentId, long paymentAttemptId) {
    }
}

package com.project.optrabidz.financial.api;

import com.project.optrabidz.common.outbox.OutboxDispatcher;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.port.PaymentWebhookReplayStore;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayEvent;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayFingerprintFactory;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentProviderWebhookApiIT extends ApiIntegrationTestSupport {
    private static final String SECRET =
            "test-only-upi-webhook-secret-material-001";
    private static final String REQUEST_ID = "webhook-security-request-123";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentWebhookReplayFingerprintFactory fingerprintFactory;

    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @SpyBean
    private PaymentWebhookReplayStore replayStore;

    @Test
    void firstDeliveryAndSequentialDuplicateReturnNoContentAndProcessOnce()
            throws Exception {
        PaymentFixture fixture = paymentFixture("sequential");
        String eventId = uniqueEventId("sequential");
        String body = confirmedBody(fixture.paymentAttemptId(), eventId, "UPI-payment-1");

        performAuthenticatedWebhook(body).andExpect(status().isNoContent());
        performAuthenticatedWebhook(body).andExpect(status().isNoContent());

        assertThat(replayCount(eventId)).isEqualTo(1L);
        assertThat(replayState(eventId)).isEqualTo("PROCESSED");
        assertThat(paymentAttemptState(fixture.paymentAttemptId())).isEqualTo("CONFIRMED");
        assertThat(outboxCount(fixture.paymentIntentId())).isEqualTo(1L);
    }

    @Test
    void concurrentIdenticalDeliveriesHaveOneOwnerAndOneAcknowledgedDuplicate()
            throws Exception {
        PaymentFixture fixture = paymentFixture("concurrent");
        String eventId = uniqueEventId("concurrent");
        String body = confirmedBody(fixture.paymentAttemptId(), eventId, "UPI-payment-2");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> responses = List.of(
                    executor.submit(() -> statusAfterSignal(body, ready, start)),
                    executor.submit(() -> statusAfterSignal(body, ready, start))
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    responses.get(0).get(15, TimeUnit.SECONDS),
                    responses.get(1).get(15, TimeUnit.SECONDS)
            )).containsExactlyInAnyOrder(204, 204);
        } finally {
            executor.shutdownNow();
        }

        assertThat(replayCount(eventId)).isEqualTo(1L);
        assertThat(outboxCount(fixture.paymentIntentId())).isEqualTo(1L);
    }

    @Test
    void reusedEventIdentityWithDifferentContentIsRejectedAndSafelyAudited()
            throws Exception {
        PaymentFixture fixture = paymentFixture("collision");
        String eventId = uniqueEventId("collision");
        performAuthenticatedWebhook(confirmedBody(
                fixture.paymentAttemptId(), eventId, "UPI-payment-winner"
        )).andExpect(status().isNoContent());

        MvcResult collision = performAuthenticatedWebhook(confirmedBody(
                fixture.paymentAttemptId(), eventId, "UPI-payment-changed"
        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(
                        "PAYMENT_WEBHOOK_PAYLOAD_INVALID"
                ))
                .andReturn();

        assertDisclosureSafe(collision);
        assertThat(collision.getResponse().getContentAsString())
                .doesNotContain(eventId)
                .doesNotContain("UPI-payment-changed");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from audit_record
                where action = 'PAYMENT_WEBHOOK_REPLAY_COLLISION'
                  and request_id = ?
                  and object_id = 'UPI'
                """, Long.class, REQUEST_ID)).isGreaterThanOrEqualTo(1L);
        assertThat(replayCount(eventId)).isEqualTo(1L);
    }

    @Test
    void unexpectedCommittedReceivedStateFailsClosedWithoutFinancialMutation()
            throws Exception {
        PaymentFixture fixture = paymentFixture("received-state");
        String eventId = uniqueEventId("received-state");
        PaymentProviderWebhookCommand command = confirmedCommand(
                fixture.paymentAttemptId(), eventId, "UPI-payment-3"
        );
        PaymentWebhookReplayEvent replayEvent = fingerprintFactory.create(command);
        jdbcTemplate.update("""
                insert into payment_webhook_event (
                    provider_code, provider_event_id, event_type,
                    processing_state, received_at, payload_hash, payload
                ) values ('UPI', ?, 'PAYMENT_CONFIRMED', 'RECEIVED', ?, ?, cast(? as jsonb))
                """,
                eventId,
                Timestamp.from(Instant.now()),
                replayEvent.payloadHash(),
                objectMapper.writeValueAsString(replayEvent.content())
        );

        performAuthenticatedWebhook(confirmedBody(
                fixture.paymentAttemptId(), eventId, "UPI-payment-3"
        )).andExpect(status().isInternalServerError());

        assertThat(paymentAttemptState(fixture.paymentAttemptId())).isEqualTo("INITIATED");
        assertThat(outboxCount(fixture.paymentIntentId())).isZero();
    }

    @Test
    void completionFailureRollsBackClaimFinancialMutationAndOutboxThenRetrySucceeds()
            throws Exception {
        PaymentFixture fixture = paymentFixture("rollback");
        String eventId = uniqueEventId("rollback");
        String body = confirmedBody(fixture.paymentAttemptId(), eventId, "UPI-payment-4");
        doThrow(new IllegalStateException("forced replay completion failure"))
                .when(replayStore)
                .markProcessed(anyLong(), anyLong(), anyLong(), any(Instant.class));

        performAuthenticatedWebhook(body).andExpect(status().isInternalServerError());

        assertThat(replayCount(eventId)).isZero();
        assertThat(paymentAttemptState(fixture.paymentAttemptId())).isEqualTo("INITIATED");
        assertThat(outboxCount(fixture.paymentIntentId())).isZero();

        reset(replayStore);
        performAuthenticatedWebhook(body).andExpect(status().isNoContent());
        assertThat(replayState(eventId)).isEqualTo("PROCESSED");
        assertThat(paymentAttemptState(fixture.paymentAttemptId())).isEqualTo("CONFIRMED");
        assertThat(outboxCount(fixture.paymentIntentId())).isEqualTo(1L);
    }

    @Test
    void providerFailureDiagnosticsRemainOutsideBusinessStateAndAudit()
            throws Exception {
        PaymentFixture fixture = paymentFixture("provider-failure");
        String eventId = uniqueEventId("provider-failure");
        String body = failedBody(
                fixture.paymentAttemptId(),
                eventId,
                "provider-secret-code",
                "provider diagnostic secret text"
        );

        performAuthenticatedWebhook(body).andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForMap("""
                select failure_code, failure_message
                from payment_attempt where payment_attempt_id = ?
                """, fixture.paymentAttemptId()))
                .containsEntry("failure_code", "PROVIDER_REPORTED_FAILURE")
                .containsEntry(
                        "failure_message",
                        "Payment provider reported that the payment failed"
                );
        String outboxEventId = outboxEventId(fixture.paymentIntentId());
        assertThat(jdbcTemplate.queryForObject(
                "select payload::text from event_outbox where event_id = ?",
                String.class,
                outboxEventId
        ))
                .doesNotContain("provider-secret-code")
                .doesNotContain("provider diagnostic secret text");

        outboxDispatcher.dispatchPending();
        outboxDispatcher.dispatchPending();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from audit_record
                where event_id = ?
                  and action = 'REPAYMENT_INSTALLMENT_PAYMENT_FAILED'
                """, Long.class, outboxEventId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("""
                select details::text from audit_record
                where event_id = ?
                  and action = 'REPAYMENT_INSTALLMENT_PAYMENT_FAILED'
                """, String.class, outboxEventId))
                .doesNotContain("provider-secret-code")
                .doesNotContain("provider diagnostic secret text");
    }

    @Test
    void unauthenticatedWebhookFailuresShareOneDisclosureSafeContract()
            throws Exception {
        String validBody = validBody();
        String currentTimestamp = String.valueOf(Instant.now().getEpochSecond());
        String staleTimestamp = String.valueOf(
                Instant.now().minusSeconds(600).getEpochSecond()
        );
        String zeroSignature = "sha256=" + "0".repeat(64);

        List<MockHttpServletRequestBuilder> requests = List.of(
                webhook("UNLISTED", validBody)
                        .header("X-Payment-Timestamp", currentTimestamp)
                        .header("X-Payment-Signature", zeroSignature),
                webhook("UPI", validBody)
                        .header("X-Payment-Timestamp", currentTimestamp),
                webhook("UPI", validBody)
                        .header("X-Payment-Timestamp", currentTimestamp)
                        .header("X-Payment-Signature", "sha256=not-hex"),
                webhook("UPI", validBody)
                        .header("X-Payment-Timestamp", staleTimestamp)
                        .header("X-Payment-Signature", zeroSignature),
                webhook("UPI", validBody)
                        .header("X-Payment-Timestamp", currentTimestamp)
                        .header(
                                "X-Payment-Signature",
                                signature(currentTimestamp, validBody + " ")
                        ),
                webhook("UPI", "{malformed-json")
                        .header("X-Payment-Timestamp", currentTimestamp)
                        .header("X-Payment-Signature", zeroSignature)
        );

        Long attemptsBefore = paymentAttemptCount();
        for (MockHttpServletRequestBuilder request : requests) {
            MvcResult result = mockMvc.perform(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(
                            MediaType.APPLICATION_PROBLEM_JSON
                    ))
                    .andExpect(jsonPath("$.code").value(
                            "PAYMENT_WEBHOOK_REJECTED"
                    ))
                    .andExpect(jsonPath("$.detail").value(
                            "The webhook request was rejected"
                    ))
                    .andExpect(jsonPath("$.success").doesNotExist())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andReturn();

            assertDisclosureSafe(result);
        }
        assertThat(paymentAttemptCount()).isEqualTo(attemptsBefore);
    }

    @Test
    void authenticatedInvalidPayloadsUseThePayloadInvalidContract()
            throws Exception {
        List<String> invalidBodies = List.of(
                """
                {"eventType":"PAYMENT_CONFIRMED","eventType":"PAYMENT_FAILED",
                 "paymentAttemptId":1001,"providerPaymentId":"UPI-1001",
                 "providerEventId":"evt-1001"}
                """,
                """
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":1001,
                 "providerPaymentId":"UPI-1001","providerEventId":"evt-1001",
                 "unexpected":"must-not-be-accepted"}
                """,
                """
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":"1001",
                 "providerPaymentId":"UPI-1001","providerEventId":"evt-1001"}
                """
        );

        Long attemptsBefore = paymentAttemptCount();
        for (String body : invalidBodies) {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            MvcResult result = mockMvc.perform(
                            webhook("UPI", body)
                                    .header("X-Payment-Timestamp", timestamp)
                                    .header(
                                            "X-Payment-Signature",
                                            signature(timestamp, body)
                                    )
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(
                            MediaType.APPLICATION_PROBLEM_JSON
                    ))
                    .andExpect(jsonPath("$.code").value(
                            "PAYMENT_WEBHOOK_PAYLOAD_INVALID"
                    ))
                    .andExpect(jsonPath("$.detail").value(
                            "The webhook payload is invalid"
                    ))
                    .andExpect(jsonPath("$.success").doesNotExist())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andReturn();

            assertDisclosureSafe(result);
        }
        assertThat(paymentAttemptCount()).isEqualTo(attemptsBefore);
    }

    private org.springframework.test.web.servlet.ResultActions
    performAuthenticatedWebhook(String body) throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        return mockMvc.perform(
                webhook("UPI", body)
                        .header("X-Payment-Timestamp", timestamp)
                        .header("X-Payment-Signature", signature(timestamp, body))
        );
    }

    private int statusAfterSignal(
            String body,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return performAuthenticatedWebhook(body)
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private PaymentFixture paymentFixture(String label) {
        Instant now = Instant.now();
        PostgresTestDataFixture.PaymentReference reference =
                new PostgresTestDataFixture(jdbcTemplate, now)
                        .createRepaymentInstallmentReference(
                                "kan32-api-" + label + "-" + UUID.randomUUID()
                        );
        Long paymentIntentId = jdbcTemplate.queryForObject("""
                insert into payment_intent (
                    payment_purpose, settlement_id, repayment_installment_id,
                    payer_account_id, payee_account_id, amount, currency_code,
                    payment_state, idempotency_key, created_at, expires_at
                ) values (
                    'REPAYMENT', null, ?, ?, ?, 550000.00, 'INR',
                    'PAYMENT_PENDING', ?, ?, ?
                ) returning payment_intent_id
                """, Long.class,
                reference.referenceId(),
                reference.payerAccountId(),
                reference.payeeAccountId(),
                "kan32-api-" + UUID.randomUUID(),
                Timestamp.from(now.minusSeconds(30)),
                Timestamp.from(now.plusSeconds(900))
        );
        Long paymentAttemptId = jdbcTemplate.queryForObject("""
                insert into payment_attempt (
                    payment_intent_id, provider_code, method_type,
                    provider_order_id, attempt_state, created_at, initiated_at,
                    provider_payload
                ) values (
                    ?, 'UPI', 'UPI', ?, 'INITIATED', ?, ?, cast('{}' as jsonb)
                ) returning payment_attempt_id
                """, Long.class,
                paymentIntentId,
                "kan32-order-" + UUID.randomUUID(),
                Timestamp.from(now.minusSeconds(20)),
                Timestamp.from(now.minusSeconds(10))
        );
        return new PaymentFixture(paymentIntentId, paymentAttemptId);
    }

    private String confirmedBody(
            long paymentAttemptId,
            String providerEventId,
            String providerPaymentId) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "eventType", "PAYMENT_CONFIRMED",
                "paymentAttemptId", paymentAttemptId,
                "providerPaymentId", providerPaymentId,
                "providerEventId", providerEventId
        ));
    }

    private String failedBody(
            long paymentAttemptId,
            String providerEventId,
            String failureCode,
            String failureMessage) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "eventType", "PAYMENT_FAILED",
                "paymentAttemptId", paymentAttemptId,
                "providerEventId", providerEventId,
                "failureCode", failureCode,
                "failureMessage", failureMessage
        ));
    }

    private PaymentProviderWebhookCommand confirmedCommand(
            long paymentAttemptId,
            String providerEventId,
            String providerPaymentId) {
        return new PaymentProviderWebhookCommand(
                "UPI",
                PaymentProviderWebhookEventType.PAYMENT_CONFIRMED,
                paymentAttemptId,
                providerPaymentId,
                null,
                null,
                providerEventId
        );
    }

    private String uniqueEventId(String label) {
        return "kan32-" + label + "-" + UUID.randomUUID();
    }

    private Long replayCount(String eventId) {
        return jdbcTemplate.queryForObject("""
                select count(*) from payment_webhook_event
                where provider_code = 'UPI' and provider_event_id = ?
                """, Long.class, eventId);
    }

    private String replayState(String eventId) {
        return jdbcTemplate.queryForObject("""
                select processing_state::text from payment_webhook_event
                where provider_code = 'UPI' and provider_event_id = ?
                """, String.class, eventId);
    }

    private String paymentAttemptState(long paymentAttemptId) {
        return jdbcTemplate.queryForObject("""
                select attempt_state::text from payment_attempt
                where payment_attempt_id = ?
                """, String.class, paymentAttemptId);
    }

    private Long outboxCount(long paymentIntentId) {
        return jdbcTemplate.queryForObject("""
                select count(*) from event_outbox
                where payload ->> 'paymentIntentId' = ?
                """, Long.class, String.valueOf(paymentIntentId));
    }

    private String outboxEventId(long paymentIntentId) {
        return jdbcTemplate.queryForObject("""
                select event_id from event_outbox
                where payload ->> 'paymentIntentId' = ?
                """, String.class, String.valueOf(paymentIntentId));
    }

    private MockHttpServletRequestBuilder webhook(String providerCode,
                                                  String body) {
        return post("/api/v1/payment-providers/{providerCode}/webhooks", providerCode)
                .header("X-Request-Id", REQUEST_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String signature(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        byte[] digest = mac.doFinal(
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8)
        );
        return "sha256=" + HexFormat.of().formatHex(digest);
    }

    private void assertDisclosureSafe(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        assertThat(response)
                .doesNotContain(SECRET)
                .doesNotContain("not-hex")
                .doesNotContain("must-not-be-accepted")
                .doesNotContain("SIGNATURE_INVALID")
                .doesNotContain("TIMESTAMP_STALE")
                .doesNotContain("PROVIDER_UNAVAILABLE")
                .doesNotContain("PaymentWebhook")
                .doesNotContain("Exception")
                .doesNotContain("stackTrace");
    }

    private Long paymentAttemptCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from payment_attempt",
                Long.class
        );
    }

    private String validBody() {
        return """
                {"eventType":"PAYMENT_CONFIRMED","paymentAttemptId":1001,
                 "providerPaymentId":"UPI-1001","providerEventId":"evt-1001"}
                """;
    }

    private record PaymentFixture(long paymentIntentId, long paymentAttemptId) {
    }
}

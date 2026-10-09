package com.project.optrabidz.financial.api;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import com.project.optrabidz.financial.application.FinancialService;
import com.project.optrabidz.financial.infrastructure.provider.PayoutOrchestrationProperties;
import com.project.optrabidz.common.outbox.OutboxDispatcher;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.testsupport.ApiIntegrationTestSupport;
import com.project.optrabidz.testsupport.ConfirmedPayoutTestConfiguration;
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import com.project.optrabidz.testsupport.PostgresTestDataFixture.PaymentReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(ConfirmedPayoutTestConfiguration.class)
class FinancialApiIT extends ApiIntegrationTestSupport {
    private static final String UPI_WEBHOOK_SECRET =
            "test-only-upi-webhook-secret-material-001";
    private static final String CARD_WEBHOOK_SECRET =
            "test-only-card-webhook-secret-material-001";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FinancialService financialService;

    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @Autowired
    private PayoutOrchestrationProperties payoutOrchestrationProperties;

    @Test
    void compatibilityModeConfirmsSettlementAndRepaymentWithoutPayoutPendingStates() throws Exception {
        payoutOrchestrationProperties.setEnabled(false);
        try {
            FinanceScenario scenario = createAcceptedBidScenario(
                    "Compatibility Startup",
                    "Compatibility Investor",
                    new BigDecimal("410000.00")
            );
            Long settlementId = getInvestorSettlementId(scenario.investor());
            Long settlementIntentId = createSettlementPaymentIntent(
                    scenario.investor(), settlementId);
            Long settlementAttemptId = createPaymentAttempt(
                    scenario.investor(), settlementIntentId);

            mockMvc.perform(post(
                            "/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm",
                            settlementAttemptId)
                            .session(scenario.investor().session())
                            .cookie(scenario.investor().xsrfCookie())
                            .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptState").value("CONFIRMED"));

            assertThat(jdbcTemplate.queryForObject("""
                    select settlement_state::text
                    from settlement
                    where settlement_id = ?
                    """, String.class, settlementId)).isEqualTo("SETTLEMENT_CONFIRMED");
            assertThat(count("""
                    select count(*) from payout_transfer where payment_intent_id = ?
                    """, settlementIntentId)).isZero();
            assertThat(count("""
                    select count(*) from event_outbox
                    where event_type = 'PaymentCollectionConfirmedEvent'
                      and payload ->> 'paymentIntentId' = ?
                    """, settlementIntentId.toString())).isZero();
            assertThat(count("""
                    select count(*) from event_outbox
                    where event_type = 'SettlementConfirmedEvent'
                      and payload ->> 'paymentIntentId' = ?
                    """, settlementIntentId.toString())).isEqualTo(1);

            Long repaymentId = getStartupRepaymentId(scenario.startup());
            Long repaymentIntentId = createRepaymentPaymentIntent(
                    scenario.startup(), repaymentId);
            Long repaymentAttemptId = createPaymentAttempt(
                    scenario.startup(), repaymentIntentId);

            mockMvc.perform(post(
                            "/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm",
                            repaymentAttemptId)
                            .session(scenario.startup().session())
                            .cookie(scenario.startup().xsrfCookie())
                            .header("X-CSRF-TOKEN", scenario.startup().csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptState").value("CONFIRMED"));

            assertThat(count("""
                    select count(*) from repayment_installment
                    where repayment_id = ? and installment_status = 'PAID'
                    """, repaymentId)).isEqualTo(1);
            assertThat(count("""
                    select count(*) from repayment_installment
                    where repayment_id = ? and installment_status = 'PAYOUT_PENDING'
                    """, repaymentId)).isZero();
            assertThat(count("""
                    select count(*) from payout_transfer where payment_intent_id = ?
                    """, repaymentIntentId)).isZero();
            assertThat(count("""
                    select count(*) from event_outbox
                    where event_type = 'PaymentCollectionConfirmedEvent'
                      and payload ->> 'paymentIntentId' = ?
                    """, repaymentIntentId.toString())).isZero();
            assertThat(count("""
                    select count(*) from event_outbox
                    where event_type = 'RepaymentInstallmentPaidEvent'
                      and payload ->> 'paymentIntentId' = ?
                    """, repaymentIntentId.toString())).isEqualTo(1);
        } finally {
            payoutOrchestrationProperties.setEnabled(true);
        }
    }

    @Test
    void scheduledOverdueTransitionPublishesOnlyOnce() {
        Instant now = Instant.parse("2026-09-29T12:00:00Z");
        PaymentReference reference = new PostgresTestDataFixture(jdbcTemplate, now)
                .createRepaymentInstallmentReference("scheduled overdue event");
        jdbcTemplate.update("""
                update repayment_installment
                set due_at = ?, updated_at = ?
                where repayment_installment_id = ?
                """, java.sql.Timestamp.from(now.minusSeconds(60)),
                java.sql.Timestamp.from(now), reference.referenceId());

        assertThat(financialService.markOverdueRepaymentInstallments(now, 10)).isEqualTo(1);
        assertThat(financialService.markOverdueRepaymentInstallments(now, 10)).isZero();

        assertThat(count("""
                select count(*) from event_outbox
                where event_type = 'RepaymentInstallmentOverdueEvent'
                  and payload ->> 'repaymentInstallmentId' = ?
                  and payload ->> 'source' = 'SCHEDULE'
                """, reference.referenceId().toString())).isEqualTo(1);
    }

    @Test
    void expiryPublishesOverdueOnlyWhenInstallmentIsDue() {
        Instant now = Instant.parse("2026-09-29T12:00:00Z");
        PostgresTestDataFixture fixture = new PostgresTestDataFixture(jdbcTemplate, now);
        PaymentReference future = fixture.createRepaymentInstallmentReference("future expiry event");
        PaymentReference due = fixture.createRepaymentInstallmentReference("due expiry event");
        prepareExpiringRepaymentIntent(future, now, now.plusSeconds(60), "future-expiry");
        prepareExpiringRepaymentIntent(due, now, now.minusSeconds(60), "due-expiry");

        assertThat(financialService.expirePendingPaymentIntents(now, 10)).isEqualTo(2);

        assertThat(count("""
                select count(*) from event_outbox
                where event_type = 'RepaymentInstallmentOverdueEvent'
                  and payload ->> 'repaymentInstallmentId' = ?
                """, future.referenceId().toString())).isZero();
        assertThat(count("""
                select count(*) from event_outbox
                where event_type = 'RepaymentInstallmentOverdueEvent'
                  and payload ->> 'repaymentInstallmentId' = ?
                  and payload ->> 'source' = 'PAYMENT_INTENT_EXPIRY'
                """, due.referenceId().toString())).isEqualTo(1);
    }

    @Test
    void missingAndNonOwnedPaymentIntentsHaveIndistinguishableProblemDetails() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Intent Scope Startup",
                "Finance Intent Scope Investor",
                new BigDecimal("545432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        AuthenticatedClient unrelatedInvestor = eligibleInvestor("Finance Unrelated Intent Investor");

        MvcResult missing = mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", 9_999_999_991L)
                        .header("X-Request-ID", "payment-intent-missing")
                        .session(unrelatedInvestor.session())
                        .cookie(unrelatedInvestor.xsrfCookie()))
                .andExpect(status().isNotFound())
                .andExpectAll(paymentProblem(
                        404,
                        "Resource not found",
                        "PAYMENT_INTENT_NOT_FOUND",
                        "The requested payment intent was not found",
                        "payment-intent-missing"
                ))
                .andReturn();
        MvcResult nonOwned = mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", paymentIntentId)
                        .header("X-Request-ID", "payment-intent-non-owned")
                        .session(unrelatedInvestor.session())
                        .cookie(unrelatedInvestor.xsrfCookie()))
                .andExpect(status().isNotFound())
                .andExpectAll(paymentProblem(
                        404,
                        "Resource not found",
                        "PAYMENT_INTENT_NOT_FOUND",
                        "The requested payment intent was not found",
                        "payment-intent-non-owned"
                ))
                .andReturn();

        assertThat(stableProblem(missing)).isEqualTo(stableProblem(nonOwned));
    }

    @Test
    void missingAndNonOwnedPaymentAttemptsHaveIndistinguishableProblemDetails() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Attempt Scope Startup",
                "Finance Attempt Scope Investor",
                new BigDecimal("535432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        Long paymentAttemptId = createPaymentAttempt(scenario.investor(), paymentIntentId);
        AuthenticatedClient unrelatedInvestor = eligibleInvestor("Finance Unrelated Attempt Investor");

        MvcResult missing = mockMvc.perform(post(
                                "/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm",
                                9_999_999_992L)
                        .header("X-Request-ID", "payment-attempt-missing")
                        .session(unrelatedInvestor.session())
                        .cookie(unrelatedInvestor.xsrfCookie())
                        .header("X-CSRF-TOKEN", unrelatedInvestor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpectAll(paymentProblem(
                        404,
                        "Resource not found",
                        "PAYMENT_ATTEMPT_NOT_FOUND",
                        "The requested payment attempt was not found",
                        "payment-attempt-missing"
                ))
                .andReturn();
        MvcResult nonOwned = mockMvc.perform(post(
                                "/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm",
                                paymentAttemptId)
                        .header("X-Request-ID", "payment-attempt-non-owned")
                        .session(unrelatedInvestor.session())
                        .cookie(unrelatedInvestor.xsrfCookie())
                        .header("X-CSRF-TOKEN", unrelatedInvestor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpectAll(paymentProblem(
                        404,
                        "Resource not found",
                        "PAYMENT_ATTEMPT_NOT_FOUND",
                        "The requested payment attempt was not found",
                        "payment-attempt-non-owned"
                ))
                .andReturn();

        assertThat(stableProblem(missing)).isEqualTo(stableProblem(nonOwned));
    }

    @Test
    void administratorCanReadPaymentIntentButCannotPerformPayerMutations() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Administrator Boundary Startup",
                "Finance Administrator Boundary Investor",
                new BigDecimal("525432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        AuthenticatedClient administrator = administrator();

        mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", paymentIntentId)
                        .session(administrator.session())
                        .cookie(administrator.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").value(paymentIntentId.intValue()));

        mockMvc.perform(post("/api/v1/payment-intents/{paymentIntentId}/attempts", paymentIntentId)
                        .header("X-Request-ID", "admin-attempt-creation-denied")
                        .session(administrator.session())
                        .cookie(administrator.xsrfCookie())
                        .header("X-CSRF-TOKEN", administrator.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpectAll(paymentProblem(
                        404,
                        "Resource not found",
                        "PAYMENT_INTENT_NOT_FOUND",
                        "The requested payment intent was not found",
                        "admin-attempt-creation-denied"
                ));

        Long paymentAttemptId = createPaymentAttempt(scenario.investor(), paymentIntentId);
        for (String action : List.of("local-confirm", "local-fail")) {
            String requestId = "admin-" + action + "-denied";
            mockMvc.perform(post(
                                    "/api/v1/payment-attempts/{paymentAttemptId}/actions/{action}",
                                    paymentAttemptId,
                                    action)
                            .header("X-Request-ID", requestId)
                            .session(administrator.session())
                            .cookie(administrator.xsrfCookie())
                            .header("X-CSRF-TOKEN", administrator.csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound())
                    .andExpectAll(paymentProblem(
                            404,
                            "Resource not found",
                            "PAYMENT_ATTEMPT_NOT_FOUND",
                            "The requested payment attempt was not found",
                            requestId
                    ));
        }
    }

    @Test
    void activePaymentRuleFailuresUseAllowlistedProblemDetails() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Rule Contract Startup",
                "Finance Rule Contract Investor",
                new BigDecimal("525432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        String diagnosticSentinel = "provider-secret-diagnostic-sentinel";

        MvcResult unsupported = mockMvc.perform(post(
                                "/api/v1/payment-intents/{paymentIntentId}/attempts", paymentIntentId)
                        .header("X-Request-ID", "payment-method-unsupported")
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "providerCode", diagnosticSentinel,
                                "methodType", "OTHER"
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpectAll(paymentProblem(
                        422,
                        "Business rule violation",
                        "PAYMENT_METHOD_UNSUPPORTED",
                        "The selected payment method is not supported",
                        "payment-method-unsupported"
                ))
                .andReturn();
        assertThat(unsupported.getResponse().getContentAsString()).doesNotContain(diagnosticSentinel);

        Long providerAttemptId = readLong(createPaymentAttempt(
                scenario.investor(), paymentIntentId, "UPI", "UPI"), "/paymentAttemptId");
        mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm", providerAttemptId)
                        .header("X-Request-ID", "payment-provider-mismatch")
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpectAll(paymentProblem(
                        422,
                        "Business rule violation",
                        "PAYMENT_PROVIDER_MISMATCH",
                        "The payment attempt cannot be handled by this provider",
                        "payment-provider-mismatch"
                ));

        Long localAttemptId = createPaymentAttempt(scenario.investor(), paymentIntentId);
        mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-fail", localAttemptId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm", localAttemptId)
                        .header("X-Request-ID", "payment-state-conflict")
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpectAll(paymentProblem(
                        409,
                        "Request conflict",
                        "PAYMENT_STATE_CONFLICT",
                        "The payment state no longer permits this operation",
                        "payment-state-conflict"
                ));
        mockMvc.perform(post("/api/v1/payment-intents/{paymentIntentId}/attempts", paymentIntentId)
                        .header("X-Request-ID", "payment-intent-not-active")
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpectAll(paymentProblem(
                        409,
                        "Request conflict",
                        "PAYMENT_INTENT_NOT_ACTIVE",
                        "The payment intent is not active",
                        "payment-intent-not-active"
                ));
    }

    @Test
    void confirmedAndExpiredIntentsUseSpecificConflictContracts() throws Exception {
        FinanceScenario confirmedScenario = createAcceptedBidScenario(
                "Finance Confirmed Contract Startup",
                "Finance Confirmed Contract Investor",
                new BigDecimal("515432.10")
        );
        Long confirmedSettlementId = getInvestorSettlementId(confirmedScenario.investor());
        Long confirmedIntentId = createSettlementPaymentIntent(
                confirmedScenario.investor(), confirmedSettlementId);
        Long confirmedAttemptId = createPaymentAttempt(confirmedScenario.investor(), confirmedIntentId);
        mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm", confirmedAttemptId)
                        .session(confirmedScenario.investor().session())
                        .cookie(confirmedScenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", confirmedScenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/payment-intents/{paymentIntentId}/attempts", confirmedIntentId)
                        .header("X-Request-ID", "payment-already-confirmed")
                        .session(confirmedScenario.investor().session())
                        .cookie(confirmedScenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", confirmedScenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpectAll(paymentProblem(
                        409,
                        "Request conflict",
                        "PAYMENT_ALREADY_CONFIRMED",
                        "The payment has already been confirmed",
                        "payment-already-confirmed"
                ));

        FinanceScenario expiredScenario = createAcceptedBidScenario(
                "Finance Expired Contract Startup",
                "Finance Expired Contract Investor",
                new BigDecimal("505432.10")
        );
        Long expiredSettlementId = getInvestorSettlementId(expiredScenario.investor());
        Long expiredIntentId = createSettlementPaymentIntent(expiredScenario.investor(), expiredSettlementId);
        jdbcTemplate.update("""
                update payment_intent
                set payment_state = 'PAYMENT_EXPIRED', expired_at = expires_at
                where payment_intent_id = ?
                """, expiredIntentId);
        mockMvc.perform(post("/api/v1/payment-intents/{paymentIntentId}/attempts", expiredIntentId)
                        .header("X-Request-ID", "payment-intent-expired")
                        .session(expiredScenario.investor().session())
                        .cookie(expiredScenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", expiredScenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpectAll(paymentProblem(
                        409,
                        "Request conflict",
                        "PAYMENT_INTENT_EXPIRED",
                        "The payment intent has expired",
                        "payment-intent-expired"
                ));
    }

    @Test
    void investorSettlementPaymentCreatesRepaymentAndStartupCanConfirmRepaymentPayment() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Lifecycle Startup",
                "Finance Lifecycle Investor",
                new BigDecimal("865432.10")
        );

        Long settlementId = getInvestorSettlementId(scenario.investor());

        mockMvc.perform(get("/api/v1/settlements/{settlementId}", settlementId)
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementId").value(settlementId.intValue()))
                .andExpect(jsonPath("$.agreementId").value(scenario.agreementId().intValue()))
                .andExpect(jsonPath("$.amount").value(865432.10))
                .andExpect(jsonPath("$.currencyCode").value("INR"))
                .andExpect(jsonPath("$.debtTerms.principalAmount").value(865432.10))
                .andExpect(jsonPath("$.debtTerms.repaymentPlanType").value("INSTALLMENT_MONTHLY"))
                .andExpect(jsonPath("$.settlementState").value("SETTLEMENT_PENDING"))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.meta").doesNotExist());

        Long settlementPaymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        Long settlementAttemptId = createPaymentAttempt(scenario.investor(), settlementPaymentIntentId);

        mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm", settlementAttemptId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentAttemptId").value(settlementAttemptId.intValue()))
                .andExpect(jsonPath("$.paymentIntentId").value(settlementPaymentIntentId.intValue()))
                .andExpect(jsonPath("$.providerCode").value("LOCAL"))
                .andExpect(jsonPath("$.methodType").value("OTHER"))
                .andExpect(jsonPath("$.attemptState").value("CONFIRMED"))
                .andExpect(jsonPath("$.providerPaymentId").value("LOCAL-PAYMENT-" + settlementAttemptId));

        completePayout(settlementPaymentIntentId);

        mockMvc.perform(get("/api/v1/settlements/{settlementId}", settlementId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementState").value("SETTLEMENT_CONFIRMED"))
                .andExpect(jsonPath("$.confirmedPaymentIntentId").value(settlementPaymentIntentId.intValue()));

        Long repaymentId = getStartupRepaymentId(scenario.startup());

        mockMvc.perform(get("/api/v1/agreements/{agreementId}/repayment-progress", scenario.agreementId())
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreementId").value(scenario.agreementId().intValue()))
                .andExpect(jsonPath("$.totalInstallments").value(18))
                .andExpect(jsonPath("$.paidInstallments").value(0))
                .andExpect(jsonPath("$.unpaidInstallments").value(18))
                .andExpect(jsonPath("$.repaymentState").value("NOT_STARTED"))
                .andExpect(jsonPath("$.nextInstallmentNumber").value(1));

        mockMvc.perform(get("/api/v1/repayments/{repaymentId}", repaymentId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repaymentId").value(repaymentId.intValue()))
                .andExpect(jsonPath("$.agreementId").value(scenario.agreementId().intValue()))
                .andExpect(jsonPath("$.totalInstallments").value(18))
                .andExpect(jsonPath("$.debtTerms.principalAmount").value(865432.10))
                .andExpect(jsonPath("$.debtTerms.repaymentPlanType").value("INSTALLMENT_MONTHLY"))
                .andExpect(jsonPath("$.repaymentState").value("NOT_STARTED"));

        mockMvc.perform(get("/api/v1/repayments/{repaymentId}/installments", repaymentId)
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(18))
                .andExpect(jsonPath("$.items[0].installmentNumber").value(1))
                .andExpect(jsonPath("$.items[0].installmentState").value("NOT_STARTED"));

        mockMvc.perform(get("/api/v1/startups/me/repayment-installments")
                        .queryParam("paymentView", "YET_TO_BE_PAID")
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(18))
                .andExpect(jsonPath("$.items[0].installmentState").value("NOT_STARTED"));

        mockMvc.perform(get("/api/v1/investors/me/repayment-installments")
                        .queryParam("paymentView", "UNPAID")
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(18));

        mockMvc.perform(get("/api/v1/repayments/{repaymentId}/installments", repaymentId)
                        .queryParam("page", "-1")
                        .queryParam("size", "101")
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(100));

        mockMvc.perform(get("/api/v1/startups/me/repayment-installments")
                        .queryParam("page", "0")
                        .queryParam("size", "0")
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1));

        mockMvc.perform(get("/api/v1/investors/me/repayment-installments")
                        .queryParam("page", "0")
                        .queryParam("size", "-1")
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1));

        mockMvc.perform(get("/api/v1/repayments/{repaymentId}/installments", repaymentId)
                        .queryParam("installmentState", "NOT_STARTED")
                        .queryParam("paymentView", "UNPAID")
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.violations[0].message")
                        .value("Use either installmentState or paymentView, not both"));

        mockMvc.perform(get("/api/v1/startups/me/repayment-installments")
                        .queryParam("installmentState", "NOT_STARTED")
                        .queryParam("paymentView", "UNPAID")
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/investors/me/repayment-installments")
                        .queryParam("installmentState", "NOT_STARTED")
                        .queryParam("paymentView", "UNPAID")
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        Long repaymentPaymentIntentId = createRepaymentPaymentIntent(scenario.startup(), repaymentId);
        Long repaymentAttemptId = createPaymentAttempt(scenario.startup(), repaymentPaymentIntentId);

        mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm", repaymentAttemptId)
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.startup().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentAttemptId").value(repaymentAttemptId.intValue()))
                .andExpect(jsonPath("$.paymentIntentId").value(repaymentPaymentIntentId.intValue()))
                .andExpect(jsonPath("$.attemptState").value("CONFIRMED"));

        completePayout(repaymentPaymentIntentId);

        mockMvc.perform(get("/api/v1/repayments/{repaymentId}", repaymentId)
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repaymentState").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/v1/repayments/{repaymentId}/installments", repaymentId)
                        .queryParam("installmentState", "PAID")
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].installmentState").value("PAID"));

        mockMvc.perform(get("/api/v1/repayments/{repaymentId}/installments", repaymentId)
                        .queryParam("paymentView", "YET_TO_BE_PAID")
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(17));

        mockMvc.perform(get("/api/v1/investors/me/repayment-installments")
                        .queryParam("installmentState", "PAID")
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].installmentState").value("PAID"));

        mockMvc.perform(get("/api/v1/agreements/{agreementId}/repayment-progress", scenario.agreementId())
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInstallments").value(18))
                .andExpect(jsonPath("$.paidInstallments").value(1))
                .andExpect(jsonPath("$.unpaidInstallments").value(17))
                .andExpect(jsonPath("$.nextInstallmentNumber").value(2));
    }

    @Test
    void quarterlyRepaymentSchedulePersistsFinalInstallmentAtContractBoundary() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Quarterly Boundary Startup",
                "Quarterly Boundary Investor",
                new BigDecimal("575432.10"),
                5,
                "INSTALLMENT_QUARTERLY"
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        Long paymentAttemptId = createPaymentAttempt(scenario.investor(), paymentIntentId);

        mockMvc.perform(post(
                                "/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm",
                                paymentAttemptId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        completePayout(paymentIntentId);

        Long repaymentId = getStartupRepaymentId(scenario.startup());
        assertThat(count("""
                select count(*)
                from repayment
                where repayment_id = ?
                  and repayment_plan_type = 'INSTALLMENT_QUARTERLY'
                  and total_installments = 2
                """, repaymentId)).isEqualTo(1);
        assertThat(count("""
                select count(*)
                from repayment r
                join repayment_installment ri on ri.repayment_id = r.repayment_id
                where r.repayment_id = ?
                  and (
                    (ri.installment_number = 1 and ri.due_at = r.started_at + interval '3 months')
                    or
                    (ri.installment_number = 2 and ri.due_at = r.started_at + interval '5 months')
                  )
                """, repaymentId)).isEqualTo(2);
        assertThat(count("""
                select count(*)
                from repayment r
                join repayment_installment ri
                  on ri.repayment_id = r.repayment_id
                 and ri.installment_number = 2
                where r.repayment_id = ?
                  and r.final_due_at = ri.due_at
                  and (
                    select sum(amount)
                    from repayment_installment
                    where repayment_id = r.repayment_id
                  ) = r.total_repayable_amount
                """, repaymentId)).isEqualTo(1);
    }

    @Test
    void upiSandboxPaymentCanBeConfirmedThroughProviderWebhook() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance UPI Webhook Startup",
                "Finance UPI Webhook Investor",
                new BigDecimal("625432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        MvcResult attemptResult = createPaymentAttempt(
                scenario.investor(),
                paymentIntentId,
                "UPI",
                "UPI"
        );
        Long paymentAttemptId = readLong(attemptResult, "/paymentAttemptId");
        assertThat(readText(attemptResult, "/providerPayload")).contains("upi://pay");

        String rawPayload = json(Map.of(
                "eventType", "PAYMENT_CONFIRMED",
                "paymentAttemptId", paymentAttemptId,
                "providerPaymentId", "UPI-PAYMENT-" + paymentAttemptId,
                "providerEventId", "evt-upi-" + paymentAttemptId
        ));

        mockMvc.perform(signedWebhook("UPI", rawPayload, UPI_WEBHOOK_SECRET))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(signedWebhook("UPI", rawPayload, UPI_WEBHOOK_SECRET))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        completePayout(paymentIntentId);

        mockMvc.perform(get("/api/v1/settlements/{settlementId}", settlementId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementState").value("SETTLEMENT_CONFIRMED"))
                .andExpect(jsonPath("$.confirmedPaymentIntentId").value(paymentIntentId.intValue()));

        mockMvc.perform(get("/api/v1/startups/me/repayments")
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(scenario.startup().session())
                .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].repaymentState").value("NOT_STARTED"));
    }

    @Test
    void competingProviderConfirmationAfterIntentFinalizedIsRejected() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Competing Webhook Startup",
                "Finance Competing Webhook Investor",
                new BigDecimal("635432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        MvcResult upiAttemptResult = createPaymentAttempt(
                scenario.investor(),
                paymentIntentId,
                "UPI",
                "UPI"
        );
        MvcResult cardAttemptResult = createPaymentAttempt(
                scenario.investor(),
                paymentIntentId,
                "CARD",
                "CARD"
        );
        Long upiAttemptId = readLong(upiAttemptResult, "/paymentAttemptId");
        Long cardAttemptId = readLong(cardAttemptResult, "/paymentAttemptId");

        String upiPayload = json(Map.of(
                "eventType", "PAYMENT_CONFIRMED",
                "paymentAttemptId", upiAttemptId,
                "providerPaymentId", "UPI-PAYMENT-" + upiAttemptId,
                "providerEventId", "evt-upi-winning-" + upiAttemptId
        ));
        mockMvc.perform(signedWebhook("UPI", upiPayload, UPI_WEBHOOK_SECRET))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        String cardPayload = json(Map.of(
                "eventType", "PAYMENT_CONFIRMED",
                "paymentAttemptId", cardAttemptId,
                "providerPaymentId", "CARD-PAYMENT-" + cardAttemptId,
                "providerEventId", "evt-card-late-" + cardAttemptId
        ));
        mockMvc.perform(signedWebhook("CARD", cardPayload, CARD_WEBHOOK_SECRET)
                        .header("X-Request-ID", "payment-competing-provider"))
                .andExpect(status().isConflict())
                .andExpectAll(paymentProblem(
                        409,
                        "Request conflict",
                        "PAYMENT_ALREADY_CONFIRMED",
                        "The payment has already been confirmed",
                        "payment-competing-provider"
                ));

        completePayout(paymentIntentId);

        mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", paymentIntentId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentState").value("PAYMENT_CONFIRMED"));

        mockMvc.perform(get("/api/v1/startups/me/repayments")
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(scenario.startup().session())
                .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void cardSandboxPaymentCanBeFailedThroughProviderWebhook() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Card Webhook Startup",
                "Finance Card Webhook Investor",
                new BigDecimal("615432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        MvcResult attemptResult = createPaymentAttempt(
                scenario.investor(),
                paymentIntentId,
                "CARD",
                "CARD"
        );
        Long paymentAttemptId = readLong(attemptResult, "/paymentAttemptId");
        assertThat(readText(attemptResult, "/providerPayload")).contains("card-checkout");

        String rawPayload = json(Map.of(
                "eventType", "PAYMENT_FAILED",
                "paymentAttemptId", paymentAttemptId,
                "failureCode", "card_declined",
                "failureMessage", "Sandbox card provider declined the payment",
                "providerEventId", "evt-card-" + paymentAttemptId
        ));

        mockMvc.perform(signedWebhook("CARD", rawPayload, CARD_WEBHOOK_SECRET))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", paymentIntentId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentState").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.failureCode").value(
                        "PROVIDER_REPORTED_FAILURE"
                ));

        mockMvc.perform(get("/api/v1/settlements/{settlementId}", settlementId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementState").value("SETTLEMENT_PENDING"));
    }

    @Test
    void settlementPaymentIntentIsIdempotentWhileActive() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Idempotent Startup",
                "Finance Idempotent Investor",
                new BigDecimal("765432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());

        Long firstPaymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        Long secondPaymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);

        mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", secondPaymentIntentId)
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").value(firstPaymentIntentId.intValue()))
                .andExpect(jsonPath("$.paymentPurpose").value("SETTLEMENT"))
                .andExpect(jsonPath("$.paymentState").value("CREATED"));
    }

    @Test
    void concurrentSettlementPaymentIntentCreationReturnsSameActiveIntent() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Concurrent Settlement Startup",
                "Finance Concurrent Settlement Investor",
                new BigDecimal("775432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());

        List<Long> paymentIntentIds = runTwoPaymentIntentRequests(
                () -> createSettlementPaymentIntent(scenario.investor(), settlementId)
        );

        assertThat(paymentIntentIds).hasSize(2);
        assertThat(paymentIntentIds).containsOnly(paymentIntentIds.get(0));
        mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", paymentIntentIds.get(0))
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentPurpose").value("SETTLEMENT"))
                .andExpect(jsonPath("$.settlementId").value(settlementId.intValue()))
                .andExpect(jsonPath("$.paymentState").value("CREATED"));
    }

    @Test
    void concurrentRepaymentPaymentIntentCreationReturnsSameActiveIntent() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Concurrent Repayment Startup",
                "Finance Concurrent Repayment Investor",
                new BigDecimal("785432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long settlementPaymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        Long settlementAttemptId = createPaymentAttempt(scenario.investor(), settlementPaymentIntentId);

        mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm", settlementAttemptId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptState").value("CONFIRMED"));

        completePayout(settlementPaymentIntentId);

        Long repaymentId = getStartupRepaymentId(scenario.startup());

        List<Long> paymentIntentIds = runTwoPaymentIntentRequests(
                () -> createRepaymentPaymentIntent(scenario.startup(), repaymentId)
        );

        assertThat(paymentIntentIds).hasSize(2);
        assertThat(paymentIntentIds).containsOnly(paymentIntentIds.get(0));
        mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", paymentIntentIds.get(0))
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentPurpose").value("REPAYMENT"))
                .andExpect(jsonPath("$.repaymentInstallmentId").exists())
                .andExpect(jsonPath("$.paymentState").value("CREATED"));
    }

    @Test
    void concurrentRepaymentInstallmentPaymentIntentCreationReturnsSameActiveIntent() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Concurrent Installment Startup",
                "Finance Concurrent Installment Investor",
                new BigDecimal("715432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long settlementPaymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        Long settlementAttemptId = createPaymentAttempt(scenario.investor(), settlementPaymentIntentId);

        mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm", settlementAttemptId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptState").value("CONFIRMED"));

        completePayout(settlementPaymentIntentId);

        Long repaymentId = getStartupRepaymentId(scenario.startup());
        Long installmentId = getFirstRepaymentInstallmentId(scenario.startup(), repaymentId);

        List<Long> paymentIntentIds = runTwoPaymentIntentRequests(
                () -> createRepaymentInstallmentPaymentIntent(scenario.startup(), installmentId)
        );

        assertThat(paymentIntentIds).hasSize(2);
        assertThat(paymentIntentIds).containsOnly(paymentIntentIds.get(0));
        mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", paymentIntentIds.get(0))
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentPurpose").value("REPAYMENT"))
                .andExpect(jsonPath("$.repaymentInstallmentId").value(installmentId.intValue()))
                .andExpect(jsonPath("$.paymentState").value("CREATED"));
    }

    @Test
    void expiredByTimeRepaymentIntentCanBeReplacedImmediately() throws Exception {
        RepaymentScenario scenario = createRepaymentScenario(
                "Expired Repayment Intent Retry",
                new BigDecimal("735432.10")
        );
        Long firstIntentId = createRepaymentInstallmentPaymentIntent(
                scenario.finance().startup(), scenario.installmentId());
        int expiredByTime = jdbcTemplate.update("""
                update payment_intent
                set created_at = now() - interval '2 minutes',
                    expires_at = now() - interval '1 minute'
                where payment_intent_id = ?
                """, firstIntentId);
        assertThat(expiredByTime).isEqualTo(1);

        Long replacementIntentId = createRepaymentInstallmentPaymentIntent(
                scenario.finance().startup(), scenario.installmentId());

        assertThat(replacementIntentId).isNotEqualTo(firstIntentId);
        assertThat(jdbcTemplate.queryForObject("""
                select payment_state::text
                from payment_intent
                where payment_intent_id = ?
                """, String.class, firstIntentId))
                .isEqualTo("PAYMENT_EXPIRED");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from payment_intent
                where repayment_installment_id = ?
                  and payment_state in ('CREATED', 'PAYMENT_PENDING')
                """, Long.class, scenario.installmentId()))
                .isEqualTo(1L);
    }

    @Test
    void concurrentLocalSettlementConfirmationCreatesRepaymentOnlyOnce() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Concurrent Confirm Startup",
                "Finance Concurrent Confirm Investor",
                new BigDecimal("795432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        Long paymentAttemptId = createPaymentAttempt(scenario.investor(), paymentIntentId);

        List<Integer> statusCodes = runTwoLocalConfirmRequests(scenario.investor(), paymentAttemptId);

        assertThat(statusCodes).containsOnly(200);
        completePayout(paymentIntentId);
        mockMvc.perform(get("/api/v1/settlements/{settlementId}", settlementId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementState").value("SETTLEMENT_CONFIRMED"))
                .andExpect(jsonPath("$.confirmedPaymentIntentId").value(paymentIntentId.intValue()));

        mockMvc.perform(get("/api/v1/startups/me/repayments")
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(scenario.startup().session())
                .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].agreementId").value(scenario.agreementId().intValue()))
                .andExpect(jsonPath("$.items[0].repaymentState").value("NOT_STARTED"));

        assertThat(count("select count(*) from repayment where agreement_id = ?", scenario.agreementId()))
                .isEqualTo(1);
        assertThat(count("""
                select count(*) from event_outbox
                where event_type = 'SettlementConfirmedEvent'
                  and payload ->> 'settlementId' = ?
                """, settlementId.toString())).isEqualTo(1);
    }

    @Test
    void localPaymentFailureMarksAttemptAndIntentFailed() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Failure Startup",
                "Finance Failure Investor",
                new BigDecimal("665432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        Long paymentAttemptId = createPaymentAttempt(scenario.investor(), paymentIntentId);

        mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-fail", paymentAttemptId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentAttemptId").value(paymentAttemptId.intValue()))
                .andExpect(jsonPath("$.attemptState").value("FAILED"))
                .andExpect(jsonPath("$.failureCode").value("LOCAL_FAILURE"))
                .andExpect(jsonPath("$.failureMessage").value("Local payment failure was simulated"));

        mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", paymentIntentId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentState").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.failureCode").value("LOCAL_FAILURE"))
                .andExpect(jsonPath("$.failureMessage").value("Local payment failure was simulated"));
    }

    @Test
    void concurrentLocalFailureIsIdempotent() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Concurrent Failure Startup",
                "Finance Concurrent Failure Investor",
                new BigDecimal("655432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        Long paymentAttemptId = createPaymentAttempt(scenario.investor(), paymentIntentId);

        List<Integer> statusCodes = runTwoLocalFailRequests(scenario.investor(), paymentAttemptId);

        assertThat(statusCodes).containsOnly(200);
        mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", paymentIntentId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentState").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.failureCode").value("LOCAL_FAILURE"));

        mockMvc.perform(get("/api/v1/settlements/{settlementId}", settlementId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementState").value("SETTLEMENT_PENDING"));
    }

    @Test
    void concurrentLocalConfirmAndFailureAllowsOnlyOneTerminalOutcome() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Confirm Fail Race Startup",
                "Finance Confirm Fail Race Investor",
                new BigDecimal("645432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        Long paymentAttemptId = createPaymentAttempt(scenario.investor(), paymentIntentId);

        List<MvcResult> terminalResults = runLocalConfirmAndFailRequests(
                scenario.investor(), paymentAttemptId);
        List<Integer> statusCodes = terminalResults.stream()
                .map(result -> result.getResponse().getStatus())
                .toList();

        assertThat(statusCodes).containsExactlyInAnyOrder(200, 409);
        MvcResult losingResult = terminalResults.stream()
                .filter(result -> result.getResponse().getStatus() == 409)
                .findFirst()
                .orElseThrow();
        JsonNode losingProblem = objectMapper.readTree(
                losingResult.getResponse().getContentAsString());
        assertThat(losingProblem.path("code").asText()).isEqualTo("PAYMENT_STATE_CONFLICT");
        assertThat(losingProblem.path("detail").asText())
                .isEqualTo("The payment state no longer permits this operation");
        MvcResult intentResult = mockMvc.perform(get("/api/v1/payment-intents/{paymentIntentId}", paymentIntentId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andReturn();

        String paymentState = readText(intentResult, "/paymentState");
        if ("PAYMENT_CONFIRMED".equals(paymentState)) {
            completePayout(paymentIntentId);
            mockMvc.perform(get("/api/v1/settlements/{settlementId}", settlementId)
                            .session(scenario.investor().session())
                            .cookie(scenario.investor().xsrfCookie()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.settlementState").value("SETTLEMENT_CONFIRMED"))
                    .andExpect(jsonPath("$.confirmedPaymentIntentId").value(paymentIntentId.intValue()));

            mockMvc.perform(get("/api/v1/startups/me/repayments")
                            .queryParam("page", "1")
                            .queryParam("size", "20")
                            .session(scenario.startup().session())
                            .cookie(scenario.startup().xsrfCookie()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalItems").value(1));
            return;
        }

        assertThat(paymentState).isEqualTo("PAYMENT_FAILED");
        mockMvc.perform(get("/api/v1/settlements/{settlementId}", settlementId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementState").value("SETTLEMENT_PENDING"));

        mockMvc.perform(get("/api/v1/startups/me/repayments")
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(scenario.startup().session())
                        .cookie(scenario.startup().xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void missingAndNonOwnedSettlementsHaveIndistinguishableProblemDetails() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Settlement Scope Startup",
                "Finance Settlement Scope Investor",
                new BigDecimal("565432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        AuthenticatedClient unrelatedInvestor = eligibleInvestor("Finance Other Settlement Investor");

        MvcResult missing = mockMvc.perform(get("/api/v1/settlements/{settlementId}", Long.MAX_VALUE)
                        .header("X-Request-ID", "settlement-missing")
                        .session(unrelatedInvestor.session())
                        .cookie(unrelatedInvestor.xsrfCookie()))
                .andExpect(status().isNotFound())
                .andExpectAll(paymentProblem(
                        404,
                        "Resource not found",
                        "SETTLEMENT_NOT_FOUND",
                        "The requested settlement was not found",
                        "settlement-missing"
                ))
                .andReturn();
        MvcResult nonOwned = mockMvc.perform(get("/api/v1/settlements/{settlementId}", settlementId)
                        .header("X-Request-ID", "settlement-non-owned")
                        .session(unrelatedInvestor.session())
                        .cookie(unrelatedInvestor.xsrfCookie()))
                .andExpect(status().isNotFound())
                .andExpectAll(paymentProblem(
                        404,
                        "Resource not found",
                        "SETTLEMENT_NOT_FOUND",
                        "The requested settlement was not found",
                        "settlement-non-owned"
                ))
                .andReturn();

        assertThat(stableProblem(missing)).isEqualTo(stableProblem(nonOwned));
        assertNoSettlementDetails(missing);
        assertNoSettlementDetails(nonOwned);
    }

    @Test
    void missingAndNonOwnedSettlementIntentCreationHaveIndistinguishableProblemDetails() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Intent Scope Startup",
                "Finance Intent Scope Investor",
                new BigDecimal("555432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        AuthenticatedClient unrelatedInvestor = eligibleInvestor("Finance Other Intent Investor");

        MvcResult missing = createSettlementIntentFailure(
                unrelatedInvestor,
                Long.MAX_VALUE,
                "settlement-intent-missing",
                status().isNotFound(),
                paymentProblem(
                        404,
                        "Resource not found",
                        "SETTLEMENT_NOT_FOUND",
                        "The requested settlement was not found",
                        "settlement-intent-missing"
                )
        );
        MvcResult nonOwned = createSettlementIntentFailure(
                unrelatedInvestor,
                settlementId,
                "settlement-intent-non-owned",
                status().isNotFound(),
                paymentProblem(
                        404,
                        "Resource not found",
                        "SETTLEMENT_NOT_FOUND",
                        "The requested settlement was not found",
                        "settlement-intent-non-owned"
                )
        );

        assertThat(stableProblem(missing)).isEqualTo(stableProblem(nonOwned));
        assertNoSettlementDetails(missing);
        assertNoSettlementDetails(nonOwned);
    }

    @Test
    void owningParticipantsAndAdministratorCanReadSettlement() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Settlement Reader Startup",
                "Finance Settlement Reader Investor",
                new BigDecimal("545432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        AuthenticatedClient administrator = administrator();

        for (AuthenticatedClient reader : List.of(
                scenario.startup(),
                scenario.investor(),
                administrator
        )) {
            mockMvc.perform(get("/api/v1/settlements/{settlementId}", settlementId)
                            .session(reader.session())
                            .cookie(reader.xsrfCookie()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.settlementId").value(settlementId.intValue()));
        }
    }

    @Test
    void startupSettlementIntentDenialPrecedesSettlementLookup() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Role Boundary Startup",
                "Finance Role Boundary Investor",
                new BigDecimal("535432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());

        MvcResult realSettlement = createSettlementIntentFailure(
                scenario.startup(),
                settlementId,
                "settlement-startup-real",
                status().isForbidden(),
                paymentProblem(
                        403,
                        "Access denied",
                        "FINANCIAL_OPERATION_NOT_ALLOWED",
                        "This financial operation is not allowed",
                        "settlement-startup-real"
                )
        );
        MvcResult missingSettlement = createSettlementIntentFailure(
                scenario.startup(),
                Long.MAX_VALUE,
                "settlement-startup-missing",
                status().isForbidden(),
                paymentProblem(
                        403,
                        "Access denied",
                        "FINANCIAL_OPERATION_NOT_ALLOWED",
                        "This financial operation is not allowed",
                        "settlement-startup-missing"
                )
        );

        assertThat(stableProblem(realSettlement)).isEqualTo(stableProblem(missingSettlement));
    }

    @Test
    void owningInvestorCannotCreateIntentForInitiallyNonPayableSettlement() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Nonpayable Startup",
                "Finance Nonpayable Investor",
                new BigDecimal("525432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        jdbcTemplate.update("""
                update settlement
                set settlement_state = 'SETTLEMENT_CANCELLED',
                    cancelled_at = created_at + interval '1 millisecond'
                where settlement_id = ?
                """, settlementId);

        createSettlementIntentFailure(
                scenario.investor(),
                settlementId,
                "settlement-not-payable",
                status().isConflict(),
                paymentProblem(
                        409,
                        "Request conflict",
                        "SETTLEMENT_NOT_PAYABLE",
                        "The settlement cannot be paid in its current state",
                        "settlement-not-payable"
                )
        );
    }

    @Test
    void conditionalSettlementConflictRollsBackPaymentAndJoinedEffects() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance Settlement Conflict Startup",
                "Finance Settlement Conflict Investor",
                new BigDecimal("515432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());
        Long paymentIntentId = createSettlementPaymentIntent(scenario.investor(), settlementId);
        Long paymentAttemptId = createPaymentAttempt(scenario.investor(), paymentIntentId);
        int cancelled = jdbcTemplate.update("""
                update settlement
                set settlement_state = 'SETTLEMENT_CANCELLED',
                    cancelled_at = created_at + interval '1 millisecond'
                where settlement_id = ?
                  and settlement_state = 'SETTLEMENT_PENDING'
                """, settlementId);
        assertThat(cancelled).isEqualTo(1);

        mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm", paymentAttemptId)
                        .header("X-Request-ID", "settlement-conflict")
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", scenario.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpectAll(paymentProblem(
                        409,
                        "Request conflict",
                        "SETTLEMENT_STATE_CONFLICT",
                        "The settlement state no longer permits this operation",
                        "settlement-conflict"
                ));

        assertThat(jdbcTemplate.queryForObject("""
                select attempt_state::text from payment_attempt where payment_attempt_id = ?
                """, String.class, paymentAttemptId)).isEqualTo("INITIATED");
        assertThat(jdbcTemplate.queryForObject("""
                select payment_state::text from payment_intent where payment_intent_id = ?
                """, String.class, paymentIntentId)).isEqualTo("PAYMENT_PENDING");
        assertThat(jdbcTemplate.queryForObject("""
                select settlement_state::text from settlement where settlement_id = ?
                """, String.class, settlementId)).isEqualTo("SETTLEMENT_CANCELLED");
        assertThat(count("select count(*) from repayment where agreement_id = ?", scenario.agreementId())).isZero();
        assertThat(count("""
                select count(*) from event_outbox
                where event_type = 'SettlementConfirmedEvent'
                  and payload ->> 'settlementId' = ?
                """, settlementId.toString())).isZero();
        assertThat(count("""
                select count(*) from notification
                where event_type = 'SettlementConfirmedEvent'
                  and entity_type = 'SETTLEMENT'
                  and entity_id = ?
                """, settlementId)).isZero();
        assertThat(count("""
                select count(*) from audit_record
                where event_type = 'SettlementConfirmedEvent'
                  and action = 'SETTLEMENT_CONFIRMED'
                  and object_type = 'SETTLEMENT'
                  and object_id = ?
                """, settlementId.toString())).isZero();
    }

    @Test
    void repaymentResourcesAndPaymentActionsHideMissingVersusNonOwnedIds()
            throws Exception {
        RepaymentScenario scenario = createRepaymentScenario(
                "Repayment Disclosure",
                new BigDecimal("505432.10")
        );
        AuthenticatedClient unrelatedStartup = eligibleStartup(
                "Repayment Unrelated Startup");
        long missingId = Long.MAX_VALUE;

        assertEquivalentRepaymentProblems(
                getRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/repayments/" + missingId,
                        "repayment-missing",
                        "REPAYMENT_NOT_FOUND",
                        "The requested repayment was not found"),
                getRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/repayments/" + scenario.repaymentId(),
                        "repayment-non-owned",
                        "REPAYMENT_NOT_FOUND",
                        "The requested repayment was not found")
        );
        assertEquivalentRepaymentProblems(
                getRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/repayments/" + missingId + "/installments",
                        "repayment-installments-missing",
                        "REPAYMENT_NOT_FOUND",
                        "The requested repayment was not found"),
                getRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/repayments/" + scenario.repaymentId()
                                + "/installments",
                        "repayment-installments-non-owned",
                        "REPAYMENT_NOT_FOUND",
                        "The requested repayment was not found")
        );
        assertEquivalentRepaymentProblems(
                getRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/repayment-installments/" + missingId,
                        "repayment-installment-missing",
                        "REPAYMENT_INSTALLMENT_NOT_FOUND",
                        "The requested repayment installment was not found"),
                getRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/repayment-installments/"
                                + scenario.installmentId(),
                        "repayment-installment-non-owned",
                        "REPAYMENT_INSTALLMENT_NOT_FOUND",
                        "The requested repayment installment was not found")
        );
        assertEquivalentRepaymentProblems(
                getRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/agreements/" + missingId
                                + "/repayment-progress",
                        "repayment-progress-missing",
                        "REPAYMENT_NOT_FOUND",
                        "The requested repayment was not found"),
                getRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/agreements/" + scenario.finance().agreementId()
                                + "/repayment-progress",
                        "repayment-progress-non-owned",
                        "REPAYMENT_NOT_FOUND",
                        "The requested repayment was not found")
        );
        assertEquivalentRepaymentProblems(
                postRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/repayments/" + missingId + "/payment-intents",
                        "repayment-intent-missing",
                        "REPAYMENT_NOT_FOUND",
                        "The requested repayment was not found"),
                postRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/repayments/" + scenario.repaymentId()
                                + "/payment-intents",
                        "repayment-intent-non-owned",
                        "REPAYMENT_NOT_FOUND",
                        "The requested repayment was not found")
        );
        assertEquivalentRepaymentProblems(
                postRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/repayment-installments/" + missingId
                                + "/payment-intents",
                        "repayment-installment-intent-missing",
                        "REPAYMENT_INSTALLMENT_NOT_FOUND",
                        "The requested repayment installment was not found"),
                postRepaymentProblem(
                        unrelatedStartup,
                        "/api/v1/repayment-installments/"
                                + scenario.installmentId() + "/payment-intents",
                        "repayment-installment-intent-non-owned",
                        "REPAYMENT_INSTALLMENT_NOT_FOUND",
                        "The requested repayment installment was not found")
        );
    }

    @Test
    void repaymentRoleAndInitialStateFailuresUseExactProblemDetails()
            throws Exception {
        RepaymentScenario scenario = createRepaymentScenario(
                "Repayment Boundary",
                new BigDecimal("495432.10")
        );

        MvcResult denied = mockMvc.perform(post(
                                "/api/v1/repayments/{repaymentId}/payment-intents",
                                scenario.repaymentId())
                        .header("X-Request-ID", "repayment-role-denied")
                        .session(scenario.finance().investor().session())
                        .cookie(scenario.finance().investor().xsrfCookie())
                        .header("X-CSRF-TOKEN",
                                scenario.finance().investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpectAll(paymentProblem(
                        403,
                        "Access denied",
                        "FINANCIAL_OPERATION_NOT_ALLOWED",
                        "This financial operation is not allowed",
                        "repayment-role-denied"
                ))
                .andReturn();
        assertNoRepaymentDiagnostics(denied);

        int cancelled = jdbcTemplate.update("""
                update repayment_installment
                set installment_status = 'CANCELLED',
                    cancelled_at = greatest(
                            created_at,
                            updated_at,
                            coalesce(payment_started_at, created_at)
                        ) + interval '1 millisecond',
                    updated_at = greatest(
                            created_at,
                            updated_at,
                            coalesce(payment_started_at, created_at)
                        ) + interval '1 millisecond'
                where repayment_installment_id = ?
                  and installment_status = 'NOT_STARTED'
                """, scenario.installmentId());
        assertThat(cancelled).isEqualTo(1);

        MvcResult notPayable = mockMvc.perform(post(
                                "/api/v1/repayment-installments/{installmentId}/payment-intents",
                                scenario.installmentId())
                        .header("X-Request-ID", "repayment-not-payable")
                        .session(scenario.finance().startup().session())
                        .cookie(scenario.finance().startup().xsrfCookie())
                        .header("X-CSRF-TOKEN",
                                scenario.finance().startup().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpectAll(paymentProblem(
                        409,
                        "Request conflict",
                        "REPAYMENT_INSTALLMENT_NOT_PAYABLE",
                        "The repayment installment cannot be paid in its current state",
                        "repayment-not-payable"
                ))
                .andReturn();
        assertNoRepaymentDiagnostics(notPayable);
    }

    @Test
    void conditionalRepaymentConflictRollsBackPaymentAndJoinedEffects()
            throws Exception {
        RepaymentScenario scenario = createRepaymentScenario(
                "Repayment Rollback",
                new BigDecimal("485432.10")
        );
        Long paymentIntentId = createRepaymentInstallmentPaymentIntent(
                scenario.finance().startup(), scenario.installmentId());
        Long paymentAttemptId = createPaymentAttempt(
                scenario.finance().startup(), paymentIntentId);
        String repaymentStateBefore = jdbcTemplate.queryForObject("""
                select repayment_status::text from repayment where repayment_id = ?
                """, String.class, scenario.repaymentId());
        long outboxBefore = count("""
                select count(*) from event_outbox
                where event_type = 'RepaymentInstallmentPaidEvent'
                  and payload ->> 'repaymentInstallmentId' = ?
                """, scenario.installmentId().toString());
        long notificationBefore = count("""
                select count(*) from notification
                where event_type = 'RepaymentInstallmentPaidEvent'
                  and entity_type = 'REPAYMENT_INSTALLMENT'
                  and entity_id = ?
                """, scenario.installmentId());
        long auditBefore = count("""
                select count(*) from audit_record
                where event_type = 'RepaymentInstallmentPaidEvent'
                  and action = 'REPAYMENT_INSTALLMENT_PAID'
                  and object_type = 'REPAYMENT_INSTALLMENT'
                  and object_id = ?
                """, scenario.installmentId().toString());

        int cancelled = jdbcTemplate.update("""
                update repayment_installment
                set installment_status = 'CANCELLED',
                    cancelled_at = greatest(
                            created_at,
                            updated_at,
                            coalesce(payment_started_at, created_at)
                        ) + interval '1 millisecond',
                    updated_at = greatest(
                            created_at,
                            updated_at,
                            coalesce(payment_started_at, created_at)
                        ) + interval '1 millisecond'
                where repayment_installment_id = ?
                  and installment_status = 'PAYMENT_IN_PROGRESS'
                """, scenario.installmentId());
        assertThat(cancelled).isEqualTo(1);

        MvcResult conflict = mockMvc.perform(post(
                                "/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm",
                                paymentAttemptId)
                        .header("X-Request-ID", "repayment-state-conflict")
                        .session(scenario.finance().startup().session())
                        .cookie(scenario.finance().startup().xsrfCookie())
                        .header("X-CSRF-TOKEN",
                                scenario.finance().startup().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpectAll(paymentProblem(
                        409,
                        "Request conflict",
                        "REPAYMENT_STATE_CONFLICT",
                        "The repayment state no longer permits this operation",
                        "repayment-state-conflict"
                ))
                .andReturn();
        assertNoRepaymentDiagnostics(conflict);

        assertThat(jdbcTemplate.queryForObject("""
                select attempt_state::text from payment_attempt
                where payment_attempt_id = ?
                """, String.class, paymentAttemptId)).isEqualTo("INITIATED");
        assertThat(jdbcTemplate.queryForObject("""
                select payment_state::text from payment_intent
                where payment_intent_id = ?
                """, String.class, paymentIntentId)).isEqualTo("PAYMENT_PENDING");
        assertThat(jdbcTemplate.queryForObject("""
                select installment_status::text from repayment_installment
                where repayment_installment_id = ?
                """, String.class, scenario.installmentId()))
                .isEqualTo("CANCELLED");
        assertThat(jdbcTemplate.queryForObject("""
                select repayment_status::text from repayment where repayment_id = ?
                """, String.class, scenario.repaymentId()))
                .isEqualTo(repaymentStateBefore);
        assertThat(count("""
                select count(*) from event_outbox
                where event_type = 'RepaymentInstallmentPaidEvent'
                  and payload ->> 'repaymentInstallmentId' = ?
                """, scenario.installmentId().toString())).isEqualTo(outboxBefore);
        assertThat(count("""
                select count(*) from notification
                where event_type = 'RepaymentInstallmentPaidEvent'
                  and entity_type = 'REPAYMENT_INSTALLMENT'
                  and entity_id = ?
                """, scenario.installmentId())).isEqualTo(notificationBefore);
        assertThat(count("""
                select count(*) from audit_record
                where event_type = 'RepaymentInstallmentPaidEvent'
                  and action = 'REPAYMENT_INSTALLMENT_PAID'
                  and object_type = 'REPAYMENT_INSTALLMENT'
                  and object_id = ?
                """, scenario.installmentId().toString())).isEqualTo(auditBefore);
    }

    @Test
    void financeMutationsRequireCsrfHeader() throws Exception {
        FinanceScenario scenario = createAcceptedBidScenario(
                "Finance CSRF Startup",
                "Finance CSRF Investor",
                new BigDecimal("465432.10")
        );
        Long settlementId = getInvestorSettlementId(scenario.investor());

        mockMvc.perform(post("/api/v1/settlements/{settlementId}/payment-intents", settlementId)
                        .session(scenario.investor().session())
                        .cookie(scenario.investor().xsrfCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("Request security validation failed"));
    }

    private FinanceScenario createAcceptedBidScenario(String startupName,
                                                      String investorName,
                                                      BigDecimal amount) throws Exception {
        return createAcceptedBidScenario(
                startupName,
                investorName,
                amount,
                18,
                "INSTALLMENT_MONTHLY"
        );
    }

    private FinanceScenario createAcceptedBidScenario(
            String startupName,
            String investorName,
            BigDecimal amount,
            int tenureMonths,
            String repaymentPlanType
    ) throws Exception {
        AuthenticatedClient startup = eligibleStartup(startupName);
        AuthenticatedClient investor = eligibleInvestor(investorName);
        Long listingId = createAndPublishListing(
                startup, startupName + " Listing", amount, tenureMonths, repaymentPlanType);
        Long bidId = submitBid(investor, listingId, amount, tenureMonths, repaymentPlanType);
        Long agreementId = acceptBid(startup, bidId);
        return new FinanceScenario(startup, investor, listingId, bidId, agreementId);
    }

    private RepaymentScenario createRepaymentScenario(
            String namePrefix,
            BigDecimal amount
    ) throws Exception {
        FinanceScenario finance = createAcceptedBidScenario(
                namePrefix + " Startup",
                namePrefix + " Investor",
                amount
        );
        Long settlementId = getInvestorSettlementId(finance.investor());
        Long paymentIntentId = createSettlementPaymentIntent(
                finance.investor(), settlementId);
        Long paymentAttemptId = createPaymentAttempt(
                finance.investor(), paymentIntentId);
        mockMvc.perform(post(
                                "/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm",
                                paymentAttemptId)
                        .session(finance.investor().session())
                        .cookie(finance.investor().xsrfCookie())
                        .header("X-CSRF-TOKEN", finance.investor().csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        completePayout(paymentIntentId);
        Long repaymentId = getStartupRepaymentId(finance.startup());
        Long installmentId = getFirstRepaymentInstallmentId(
                finance.startup(), repaymentId);
        return new RepaymentScenario(finance, repaymentId, installmentId);
    }

    private AuthenticatedClient eligibleStartup(String publicDisplayName) throws Exception {
        AuthenticatedClient startup = registerAndLogin(RoleType.STARTUP);
        createCompleteStartupProfile(startup, publicDisplayName);
        addStartupClassification(startup, "SECTOR", "FINTECH");
        createAndVerifyReceivingAccount(startup);
        return startup;
    }

    private AuthenticatedClient eligibleInvestor(String publicDisplayName) throws Exception {
        AuthenticatedClient investor = registerAndLogin(RoleType.INVESTOR);
        createCompleteInvestorProfile(investor, publicDisplayName);
        addInvestorPreference(investor, "SECTOR", "FINTECH");
        createAndVerifyReceivingAccount(investor);
        return investor;
    }

    private AuthenticatedClient administrator() throws Exception {
        String email = uniqueEmail("finance-admin");
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

    private Long createAndPublishListing(AuthenticatedClient startup, String title, BigDecimal amount) throws Exception {
        return createAndPublishListing(startup, title, amount, 18, "INSTALLMENT_MONTHLY");
    }

    private Long createAndPublishListing(
            AuthenticatedClient startup,
            String title,
            BigDecimal amount,
            int tenureMonths,
            String repaymentPlanType
    ) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/funding-listings")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createListingRequest(
                                title, amount, tenureMonths, repaymentPlanType))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingState").value("DRAFT"))
                .andReturn();
        Long listingId = readLong(createResult, "/listingId");

        mockMvc.perform(post("/api/v1/funding-listings/{listingId}/actions/publish", listingId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingState").value("OPEN"));

        return listingId;
    }

    private Long submitBid(AuthenticatedClient investor, Long listingId, BigDecimal amount) throws Exception {
        return submitBid(investor, listingId, amount, 18, "INSTALLMENT_MONTHLY");
    }

    private Long submitBid(
            AuthenticatedClient investor,
            Long listingId,
            BigDecimal amount,
            int tenureMonths,
            String repaymentPlanType
    ) throws Exception {
        MvcResult bidResult = mockMvc.perform(post("/api/v1/bids")
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(submitBidRequest(
                                listingId, amount, tenureMonths, repaymentPlanType))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bidState").value("SUBMITTED"))
                .andReturn();
        return readLong(bidResult, "/bidId");
    }

    private Long acceptBid(AuthenticatedClient startup, Long bidId) throws Exception {
        MvcResult acceptResult = mockMvc.perform(post("/api/v1/bids/{bidId}/actions/accept", bidId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("confirmation", "ACCEPT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bid.bidState").value("ACCEPTED"))
                .andExpect(jsonPath("$.listing.listingState").value("AGREEMENT_REACHED"))
                .andReturn();
        return readLong(acceptResult, "/agreement/agreementId");
    }

    private Long getInvestorSettlementId(AuthenticatedClient investor) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/investors/me/settlements")
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(investor.session())
                .cookie(investor.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].settlementState").value("SETTLEMENT_PENDING"))
                .andReturn();
        return readLong(result, "/items/0/settlementId");
    }

    private Long getStartupRepaymentId(AuthenticatedClient startup) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/startups/me/repayments")
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].repaymentState").value("NOT_STARTED"))
                .andReturn();
        return readLong(result, "/items/0/repaymentId");
    }

    private void completePayout(Long paymentIntentId) {
        List<String> transferStatuses = List.of();
        for (int attempt = 0; attempt < 5; attempt++) {
            outboxDispatcher.dispatchPending();
            transferStatuses = jdbcTemplate.queryForList("""
                    select transfer_status::text
                    from payout_transfer
                    where payment_intent_id = ?
                    """, String.class, paymentIntentId);
            if (transferStatuses.contains("CONFIRMED")) {
                return;
            }
        }
        assertThat(transferStatuses)
                .as("payout transfer status for payment intent %s", paymentIntentId)
                .contains("CONFIRMED");
    }

    private Long createSettlementPaymentIntent(AuthenticatedClient investor, Long settlementId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/settlements/{settlementId}/payment-intents", settlementId)
                        .session(investor.session())
                        .cookie(investor.xsrfCookie())
                        .header("X-CSRF-TOKEN", investor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(
                        "/api/v1/payment-intents/\\d+")))
                .andExpect(jsonPath("$.paymentPurpose").value("SETTLEMENT"))
                .andExpect(jsonPath("$.settlementId").value(settlementId.intValue()))
                .andExpect(jsonPath("$.paymentState").value("CREATED"))
                .andReturn();
        return readLong(result, "/paymentIntentId");
    }

    private MvcResult getRepaymentProblem(
            AuthenticatedClient actor,
            String path,
            String requestId,
            String code,
            String detail
    ) throws Exception {
        return mockMvc.perform(get(path)
                        .header("X-Request-ID", requestId)
                        .session(actor.session())
                        .cookie(actor.xsrfCookie()))
                .andExpect(status().isNotFound())
                .andExpectAll(paymentProblem(
                        404,
                        "Resource not found",
                        code,
                        detail,
                        requestId
                ))
                .andReturn();
    }

    private MvcResult postRepaymentProblem(
            AuthenticatedClient actor,
            String path,
            String requestId,
            String code,
            String detail
    ) throws Exception {
        return mockMvc.perform(post(path)
                        .header("X-Request-ID", requestId)
                        .session(actor.session())
                        .cookie(actor.xsrfCookie())
                        .header("X-CSRF-TOKEN", actor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpectAll(paymentProblem(
                        404,
                        "Resource not found",
                        code,
                        detail,
                        requestId
                ))
                .andReturn();
    }

    private MvcResult createSettlementIntentFailure(
            AuthenticatedClient actor,
            Long settlementId,
            String requestId,
            ResultMatcher expectedStatus,
            ResultMatcher[] problem
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/settlements/{settlementId}/payment-intents", settlementId)
                        .header("X-Request-ID", requestId)
                        .session(actor.session())
                        .cookie(actor.xsrfCookie())
                        .header("X-CSRF-TOKEN", actor.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(expectedStatus)
                .andExpectAll(problem)
                .andReturn();
    }

    private Long createRepaymentPaymentIntent(AuthenticatedClient startup, Long repaymentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/repayments/{repaymentId}/payment-intents", repaymentId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(
                        "/api/v1/payment-intents/\\d+")))
                .andExpect(jsonPath("$.paymentPurpose").value("REPAYMENT"))
                .andExpect(jsonPath("$.repaymentInstallmentId").exists())
                .andExpect(jsonPath("$.paymentState").value("CREATED"))
                .andReturn();
        return readLong(result, "/paymentIntentId");
    }

    private Long getFirstRepaymentInstallmentId(AuthenticatedClient startup, Long repaymentId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/repayments/{repaymentId}/installments", repaymentId)
                        .queryParam("page", "1")
                        .queryParam("size", "20")
                        .session(startup.session())
                        .cookie(startup.xsrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(18))
                .andExpect(jsonPath("$.items[0].installmentNumber").value(1))
                .andReturn();
        return readLong(result, "/items/0/repaymentInstallmentId");
    }

    private Long createRepaymentInstallmentPaymentIntent(AuthenticatedClient startup, Long installmentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/repayment-installments/{installmentId}/payment-intents", installmentId)
                        .session(startup.session())
                        .cookie(startup.xsrfCookie())
                        .header("X-CSRF-TOKEN", startup.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(
                        "/api/v1/payment-intents/\\d+")))
                .andExpect(jsonPath("$.paymentPurpose").value("REPAYMENT"))
                .andExpect(jsonPath("$.repaymentInstallmentId").value(installmentId.intValue()))
                .andExpect(jsonPath("$.paymentState").value("CREATED"))
                .andReturn();
        return readLong(result, "/paymentIntentId");
    }

    private Long createPaymentAttempt(AuthenticatedClient payer, Long paymentIntentId) throws Exception {
        MvcResult result = createPaymentAttempt(payer, paymentIntentId, "LOCAL", "OTHER");
        return readLong(result, "/paymentAttemptId");
    }

    private MvcResult createPaymentAttempt(AuthenticatedClient payer,
                                           Long paymentIntentId,
                                           String providerCode,
                                           String methodType) throws Exception {
        return mockMvc.perform(post("/api/v1/payment-intents/{paymentIntentId}/attempts", paymentIntentId)
                        .session(payer.session())
                        .cookie(payer.xsrfCookie())
                        .header("X-CSRF-TOKEN", payer.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "providerCode", providerCode,
                                "methodType", methodType
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.paymentIntentId").value(paymentIntentId.intValue()))
                .andExpect(jsonPath("$.providerCode").value(providerCode))
                .andExpect(jsonPath("$.methodType").value(methodType))
                .andExpect(jsonPath("$.attemptState").value("INITIATED"))
                .andExpect(jsonPath("$.providerPayload").isNotEmpty())
                .andReturn();
    }

    private List<Long> runTwoPaymentIntentRequests(Callable<Long> request) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Long> synchronizedRequest = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return request.call();
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Long> first = executor.submit(synchronizedRequest);
            Future<Long> second = executor.submit(synchronizedRequest);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(), second.get());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private List<Integer> runTwoLocalConfirmRequests(AuthenticatedClient payer, Long paymentAttemptId) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> synchronizedRequest = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm", paymentAttemptId)
                            .session(payer.session())
                            .cookie(payer.xsrfCookie())
                            .header("X-CSRF-TOKEN", payer.csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(synchronizedRequest);
            Future<Integer> second = executor.submit(synchronizedRequest);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(), second.get());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private List<Integer> runTwoLocalFailRequests(AuthenticatedClient payer, Long paymentAttemptId) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> synchronizedRequest = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-fail", paymentAttemptId)
                            .session(payer.session())
                            .cookie(payer.xsrfCookie())
                            .header("X-CSRF-TOKEN", payer.csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(synchronizedRequest);
            Future<Integer> second = executor.submit(synchronizedRequest);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(), second.get());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private List<MvcResult> runLocalConfirmAndFailRequests(
            AuthenticatedClient payer,
            Long paymentAttemptId
    ) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<MvcResult> confirmRequest = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-confirm", paymentAttemptId)
                            .session(payer.session())
                            .cookie(payer.xsrfCookie())
                            .header("X-CSRF-TOKEN", payer.csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn();
        };
        Callable<MvcResult> failRequest = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return mockMvc.perform(post("/api/v1/payment-attempts/{paymentAttemptId}/actions/local-fail", paymentAttemptId)
                            .session(payer.session())
                            .cookie(payer.xsrfCookie())
                            .header("X-CSRF-TOKEN", payer.csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn();
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> confirm = executor.submit(confirmRequest);
            Future<MvcResult> fail = executor.submit(failRequest);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(confirm.get(), fail.get());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Map<String, Object> createListingRequest(String title, BigDecimal amount) {
        return createListingRequest(title, amount, 18, "INSTALLMENT_MONTHLY");
    }

    private Map<String, Object> createListingRequest(
            String title,
            BigDecimal amount,
            int tenureMonths,
            String repaymentPlanType
    ) {
        return Map.of(
                "fundingModel", "DEBT",
                "title", title,
                "fundingPurposeDescription", "Funds needed for finance module integration testing.",
                "debtTerms", Map.of(
                        "requestedAmount", amount,
                        "currencyCode", "INR",
                        "minimumInterestRate", new BigDecimal("8.50"),
                        "maximumInterestRate", new BigDecimal("12.75"),
                        "requestedTenureMonths", tenureMonths,
                        "repaymentPlanType", repaymentPlanType
                )
        );
    }

    private Map<String, Object> submitBidRequest(Long listingId, BigDecimal amount) {
        return submitBidRequest(listingId, amount, 18, "INSTALLMENT_MONTHLY");
    }

    private Map<String, Object> submitBidRequest(
            Long listingId,
            BigDecimal amount,
            int tenureMonths,
            String repaymentPlanType
    ) {
        return Map.of(
                "listingId", listingId,
                "fundingModel", "DEBT",
                "debtTerms", Map.of(
                        "proposedAmount", amount,
                        "proposedInterestRate", new BigDecimal("10.25"),
                        "proposedTenureMonths", tenureMonths,
                        "repaymentPlanType", repaymentPlanType
                ),
                "proposalMessage", "Funding offer for finance module integration testing."
        );
    }

    private Long readLong(MvcResult result, String jsonPointer) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString()).at(jsonPointer);
        return node.asLong();
    }

    private String readText(MvcResult result, String jsonPointer) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString()).at(jsonPointer);
        return node.asText();
    }

    private MockHttpServletRequestBuilder signedWebhook(String providerCode,
                                                        String rawPayload,
                                                        String secret) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        return post(
                "/api/v1/payment-providers/{providerCode}/webhooks",
                providerCode
        )
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-PAYMENT-TIMESTAMP", timestamp)
                .header(
                        "X-PAYMENT-SIGNATURE",
                        paymentSignature(timestamp, rawPayload, secret)
                )
                .content(rawPayload);
    }

    private String paymentSignature(String timestamp,
                                    String rawPayload,
                                    String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of()
                    .formatHex(mac.doFinal(
                            (timestamp + "." + rawPayload)
                                    .getBytes(StandardCharsets.UTF_8)
                    ));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ResultMatcher[] paymentProblem(
            int statusCode,
            String title,
            String code,
            String detail,
            String requestId
    ) {
        return new ResultMatcher[]{
                content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
                jsonPath("$.title").value(title),
                jsonPath("$.status").value(statusCode),
                jsonPath("$.detail").value(detail),
                jsonPath("$.instance").value("urn:optrabidz:request:" + requestId),
                jsonPath("$.code").value(code),
                jsonPath("$.requestId").value(requestId),
                jsonPath("$.timestamp").isString(),
                jsonPath("$.diagnosticCode").doesNotExist(),
                jsonPath("$.exception").doesNotExist(),
                jsonPath("$.stackTrace").doesNotExist()
        };
    }

    private JsonNode stableProblem(MvcResult result) throws Exception {
        ObjectNode problem = (ObjectNode) objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        problem.remove(List.of("requestId", "timestamp", "instance"));
        return problem;
    }

    private void assertEquivalentRepaymentProblems(
            MvcResult missing,
            MvcResult nonOwned
    ) throws Exception {
        assertThat(missing.getResponse().getContentType())
                .isEqualTo(nonOwned.getResponse().getContentType());
        assertThat(stableProblem(missing)).isEqualTo(stableProblem(nonOwned));
        assertNoRepaymentDiagnostics(missing);
        assertNoRepaymentDiagnostics(nonOwned);
    }

    private void assertNoRepaymentDiagnostics(MvcResult result)
            throws Exception {
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(
                        "protected-repayment-sentinel",
                        "RepaymentNotFoundException",
                        "RepaymentInstallmentNotFoundException",
                        "RepaymentStateConflictException",
                        "SQLException",
                        "select * from",
                        "FINANCIAL.REPAYMENT"
                );
    }

    private void assertNoSettlementDetails(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .doesNotContain(
                        "settlementState",
                        "expiresAt",
                        "investorId",
                        "startupId",
                        "amount",
                        "agreementId",
                        "FINANCIAL.SETTLEMENT"
                );
    }

    private void prepareExpiringRepaymentIntent(PaymentReference reference,
                                                Instant now,
                                                Instant dueAt,
                                                String idempotencyKey) {
        jdbcTemplate.update("""
                update repayment_installment
                set installment_status = 'PAYMENT_IN_PROGRESS',
                    due_at = ?,
                    payment_started_at = ?,
                    updated_at = ?
                where repayment_installment_id = ?
                """, java.sql.Timestamp.from(dueAt), java.sql.Timestamp.from(now.minusSeconds(90)),
                java.sql.Timestamp.from(now), reference.referenceId());
        jdbcTemplate.update("""
                insert into payment_intent (
                    payment_purpose, repayment_installment_id,
                    payer_account_id, payee_account_id,
                    amount, currency_code, payment_state, idempotency_key,
                    created_at, expires_at
                )
                values ('REPAYMENT', ?, ?, ?, 550000.00, 'INR', 'CREATED', ?, ?, ?)
                """, reference.referenceId(), reference.payerAccountId(), reference.payeeAccountId(),
                idempotencyKey, java.sql.Timestamp.from(now.minusSeconds(120)),
                java.sql.Timestamp.from(now.minusSeconds(1)));
    }

    private long count(String sql, Object... arguments) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, arguments);
        return result == null ? 0 : result;
    }

    private record FinanceScenario(
            AuthenticatedClient startup,
            AuthenticatedClient investor,
            Long listingId,
            Long bidId,
            Long agreementId
    ) {
    }

    private record RepaymentScenario(
            FinanceScenario finance,
            Long repaymentId,
            Long installmentId
    ) {
    }
}

package com.project.optrabidz.financial.domain.model;

import org.junit.jupiter.api.Test;

import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialDomainTest {
    private static final Instant NOW = Instant.parse("2026-05-19T10:00:00Z");

    @Test
    void pendingSettlementCanBeConfirmedOnlyOnce() {
        Settlement settlement = settlement();
        Instant confirmedAt = NOW.plusSeconds(300);

        settlement.markConfirmed(9001L, confirmedAt);

        assertThat(settlement.getSettlementState()).isEqualTo(SettlementState.SETTLEMENT_CONFIRMED);
        assertThat(settlement.getConfirmedPaymentIntentId()).isEqualTo(9001L);
        assertThat(settlement.getConfirmedAt()).isEqualTo(confirmedAt);

        assertThatThrownBy(() -> settlement.markFailed("late failure", NOW.plusSeconds(360)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending settlement can be failed");
    }

    @Test
    void settlementExpiresOnlyWhenPendingAndPastExpiry() {
        Settlement settlement = settlement();

        assertThat(settlement.expireIfEligible(NOW.plusSeconds(60))).isFalse();
        assertThat(settlement.getSettlementState()).isEqualTo(SettlementState.SETTLEMENT_PENDING);

        assertThat(settlement.expireIfEligible(NOW.plusSeconds(1_200))).isTrue();
        assertThat(settlement.getSettlementState()).isEqualTo(SettlementState.SETTLEMENT_EXPIRED);
        assertThat(settlement.getExpiredAt()).isEqualTo(NOW.plusSeconds(1_200));

        assertThat(settlement.expireIfEligible(NOW.plusSeconds(1_300))).isFalse();
    }

    @Test
    void settlementCanBeCancelledOnlyWhilePending() {
        Settlement settlement = settlement();
        settlement.cancel(NOW.plusSeconds(120));

        assertThat(settlement.getSettlementState()).isEqualTo(SettlementState.SETTLEMENT_CANCELLED);
        assertThat(settlement.getCancelledAt()).isEqualTo(NOW.plusSeconds(120));

        assertThatThrownBy(() -> settlement.markConfirmed(9001L, NOW.plusSeconds(180)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending settlement can be confirmed");
    }

    @Test
    void paymentIntentMovesFromCreatedToPendingToConfirmed() {
        PaymentIntent intent = settlementPaymentIntent();

        intent.markPending();
        assertThat(intent.getPaymentState()).isEqualTo(PaymentState.PAYMENT_PENDING);

        intent.markConfirmed(NOW.plusSeconds(60));

        assertThat(intent.getPaymentState()).isEqualTo(PaymentState.PAYMENT_CONFIRMED);
        assertThat(intent.getConfirmedAt()).isEqualTo(NOW.plusSeconds(60));

        assertThatThrownBy(() -> intent.markFailed("FAILED", "Already confirmed", NOW.plusSeconds(90)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only active payment intent can be failed");
    }

    @Test
    void paymentIntentRejectsSamePayerAndPayee() {
        assertThatThrownBy(() -> PaymentIntent.forSettlement(
                1L,
                10L,
                10L,
                new BigDecimal("550000.00"),
                "INR",
                "idem-001",
                NOW,
                NOW.plusSeconds(900)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payerAccountId and payeeAccountId must differ");
    }

    @Test
    void paymentIntentExpiresOnlyWhileActiveAndPastExpiry() {
        PaymentIntent intent = settlementPaymentIntent();

        assertThat(intent.expireIfEligible(NOW.plusSeconds(60))).isFalse();
        assertThat(intent.getPaymentState()).isEqualTo(PaymentState.CREATED);

        assertThat(intent.expireIfEligible(NOW.plusSeconds(1_000))).isTrue();
        assertThat(intent.getPaymentState()).isEqualTo(PaymentState.PAYMENT_EXPIRED);
        assertThat(intent.getExpiredAt()).isEqualTo(NOW.plusSeconds(1_000));

        assertThatThrownBy(() -> intent.markConfirmed(NOW.plusSeconds(1_100)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only active payment intent can be confirmed");
    }

    @Test
    void paymentAttemptCanBeInitiatedAndConfirmed() {
        PaymentAttempt attempt = PaymentAttempt.create(9001L, "LOCAL", PaymentMethodType.OTHER, NOW);

        attempt.markInitiated("order-001", "ref-001", "{\"mode\":\"local\"}", NOW.plusSeconds(10));

        assertThat(attempt.getAttemptState()).isEqualTo(PaymentAttemptState.INITIATED);
        assertThat(attempt.getProviderOrderId()).isEqualTo("order-001");
        assertThat(attempt.getProviderReferenceId()).isEqualTo("ref-001");

        attempt.markConfirmed("payment-001", NOW.plusSeconds(30));

        assertThat(attempt.getAttemptState()).isEqualTo(PaymentAttemptState.CONFIRMED);
        assertThat(attempt.getProviderPaymentId()).isEqualTo("payment-001");
        assertThat(attempt.getConfirmedAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void finalPaymentAttemptCannotBeFailed() {
        PaymentAttempt attempt = PaymentAttempt.create(9001L, "LOCAL", PaymentMethodType.OTHER, NOW);
        attempt.markConfirmed("payment-001", NOW.plusSeconds(30));

        assertThatThrownBy(() -> attempt.markFailed("FAILED", "Gateway callback arrived late", NOW.plusSeconds(40)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Final payment attempt cannot be failed");
    }

    @Test
    void repaymentAggregateStartsAsNotStartedAndDoesNotBehaveLikeInstallment() {
        Repayment repayment = repayment();

        assertThat(repayment.getRepaymentState()).isEqualTo(RepaymentState.NOT_STARTED);
        assertThat(repayment.getTotalInstallments()).isEqualTo(6);
        assertThat(repayment.getTotalRepayableAmount()).isEqualByComparingTo("585000.00");
        assertThat(repayment.getFinalDueAt()).isEqualTo(NOW.plusSeconds(9_000));
    }

    @Test
    void repaymentInstallmentTracksIndividualPaymentUnit() {
        RepaymentInstallment installment = RepaymentInstallment.create(
                8001L,
                1,
                new BigDecimal("97500.00"),
                "INR",
                NOW.plusSeconds(7_200),
                NOW
        );

        assertThat(installment.getRepaymentId()).isEqualTo(8001L);
        assertThat(installment.getInstallmentNumber()).isEqualTo(1);
        assertThat(installment.getInstallmentState()).isEqualTo(RepaymentInstallmentState.NOT_STARTED);
        assertThat(installment.getAmount()).isEqualByComparingTo("97500.00");
        assertThat(installment.getDueAt()).isEqualTo(NOW.plusSeconds(7_200));
    }

    private static Settlement settlement() {
        return Settlement.create(
                7001L,
                11L,
                22L,
                new BigDecimal("550000.00"),
                "INR",
                NOW,
                NOW.plusSeconds(900)
        );
    }

    private static PaymentIntent settlementPaymentIntent() {
        return PaymentIntent.forSettlement(
                1L,
                22L,
                11L,
                new BigDecimal("550000.00"),
                "INR",
                "settlement-1",
                NOW,
                NOW.plusSeconds(900)
        );
    }

    private static Repayment repayment() {
        return Repayment.create(
                7001L,
                11L,
                22L,
                new BigDecimal("585000.00"),
                "INR",
                6,
                RepaymentPlanType.INSTALLMENT_QUARTERLY,
                NOW,
                NOW.plusSeconds(9_000),
                NOW
        );
    }
}

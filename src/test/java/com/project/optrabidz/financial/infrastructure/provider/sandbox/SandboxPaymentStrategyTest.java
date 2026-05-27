package com.project.optrabidz.financial.infrastructure.provider.sandbox;

import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxPaymentStrategyTest {
    private static final Instant NOW = Instant.parse("2026-05-21T00:00:00Z");

    @Test
    void upiStrategySupportsOnlyUpiProviderAndMethod() {
        SandboxUpiPaymentStrategy strategy = new SandboxUpiPaymentStrategy();

        assertThat(strategy.supports("upi", PaymentMethodType.UPI)).isTrue();
        assertThat(strategy.supports("UPI", PaymentMethodType.CARD)).isFalse();
        assertThat(strategy.supports("CARD", PaymentMethodType.UPI)).isFalse();
    }

    @Test
    void upiStrategyInitiatesAttemptWithCollectPayload() {
        SandboxUpiPaymentStrategy strategy = new SandboxUpiPaymentStrategy();

        PaymentAttempt attempt = strategy.initiate(paymentIntent(), paymentAttempt("UPI", PaymentMethodType.UPI), NOW);

        assertThat(attempt.getAttemptState()).isEqualTo(PaymentAttemptState.INITIATED);
        assertThat(attempt.getProviderOrderId()).isEqualTo("UPI-ORDER-1001");
        assertThat(attempt.getProviderReferenceId()).isEqualTo("UPI-REF-1001");
        assertThat(attempt.getProviderPayload()).contains("\"provider\":\"UPI\"");
        assertThat(attempt.getProviderPayload()).contains("upi://pay");
    }

    @Test
    void cardStrategyInitiatesAttemptWithCheckoutPayload() {
        SandboxCardPaymentStrategy strategy = new SandboxCardPaymentStrategy();

        PaymentAttempt attempt = strategy.initiate(paymentIntent(), paymentAttempt("CARD", PaymentMethodType.CARD), NOW);

        assertThat(attempt.getAttemptState()).isEqualTo(PaymentAttemptState.INITIATED);
        assertThat(attempt.getProviderOrderId()).isEqualTo("CARD-ORDER-1001");
        assertThat(attempt.getProviderReferenceId()).isEqualTo("CARD-REF-1001");
        assertThat(attempt.getProviderPayload()).contains("\"provider\":\"CARD\"");
        assertThat(attempt.getProviderPayload()).contains("card-checkout/1001");
    }

    private PaymentIntent paymentIntent() {
        return PaymentIntent.builder()
                .paymentIntentId(901L)
                .paymentPurpose(PaymentPurpose.SETTLEMENT)
                .settlementId(501L)
                .payerAccountId(101L)
                .payeeAccountId(202L)
                .amount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .paymentState(PaymentState.CREATED)
                .idempotencyKey("SETTLEMENT-501")
                .createdAt(NOW.minusSeconds(30))
                .expiresAt(NOW.plusSeconds(900))
                .build();
    }

    private PaymentAttempt paymentAttempt(String providerCode, PaymentMethodType methodType) {
        return PaymentAttempt.builder()
                .paymentAttemptId(1001L)
                .paymentIntentId(901L)
                .providerCode(providerCode)
                .methodType(methodType)
                .attemptState(PaymentAttemptState.CREATED)
                .createdAt(NOW)
                .build();
    }
}

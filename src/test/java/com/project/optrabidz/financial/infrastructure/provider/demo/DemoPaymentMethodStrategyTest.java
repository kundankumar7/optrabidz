package com.project.optrabidz.financial.infrastructure.provider.demo;

import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DemoPaymentMethodStrategyTest {
    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");

    @Test
    void supportsOnlyDemoUpiAndCardAndInitiatesWithoutRealPaymentData() {
        DemoPaymentMethodStrategy strategy = new DemoPaymentMethodStrategy();

        assertThat(strategy.supports("DEMO", PaymentMethodType.UPI)).isTrue();
        assertThat(strategy.supports("demo", PaymentMethodType.CARD)).isTrue();
        assertThat(strategy.supports("DEMO", PaymentMethodType.OTHER)).isFalse();
        assertThat(strategy.supports("LOCAL", PaymentMethodType.UPI)).isFalse();

        PaymentAttempt attempt = PaymentAttempt.builder()
                .paymentAttemptId(10L)
                .paymentIntentId(20L)
                .providerCode("DEMO")
                .methodType(PaymentMethodType.UPI)
                .attemptState(PaymentAttemptState.CREATED)
                .createdAt(NOW)
                .build();
        PaymentIntent intent = PaymentIntent.forSettlement(
                30L, 40L, 50L, new BigDecimal("100.00"), "INR",
                "intent-key", NOW, NOW.plusSeconds(600));

        PaymentAttempt initiated = strategy.initiate(intent, attempt, NOW.plusSeconds(1));

        assertThat(initiated.getAttemptState()).isEqualTo(PaymentAttemptState.INITIATED);
        assertThat(initiated.getProviderOrderId()).isEqualTo("DEMO-ORDER-10");
        assertThat(initiated.getProviderReferenceId()).isEqualTo("DEMO-CHECKOUT-10");
        assertThat(initiated.getProviderPayload()).isEqualTo("{\"environment\":\"DEMONSTRATION\"}");
    }
}

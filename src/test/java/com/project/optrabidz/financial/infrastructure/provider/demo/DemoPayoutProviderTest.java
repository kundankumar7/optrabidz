package com.project.optrabidz.financial.infrastructure.provider.demo;

import com.project.optrabidz.financial.application.payout.PayoutInstruction;
import com.project.optrabidz.financial.application.payout.PayoutOutcome;
import com.project.optrabidz.financial.application.payout.PayoutResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DemoPayoutProviderTest {
    @Test
    void successBehaviorConfirmsWithDeterministicProviderReference() {
        DemoPaymentProviderProperties properties = properties(DemoPayoutBehavior.SUCCESS);
        DemoPayoutProvider provider = new DemoPayoutProvider(properties);

        PayoutResult result = provider.execute(instruction(1));

        assertThat(result.outcome()).isEqualTo(PayoutOutcome.CONFIRMED);
        assertThat(result.providerReference()).isEqualTo("DEMO-PAYOUT-41");
        assertThat(result.failureCode()).isNull();
        assertThat(result.failureMessage()).isNull();
    }

    @Test
    void failFirstAttemptBehaviorFailsOnceAndThenConfirms() {
        DemoPaymentProviderProperties properties = properties(DemoPayoutBehavior.FAIL_FIRST_ATTEMPT);
        DemoPayoutProvider provider = new DemoPayoutProvider(properties);

        PayoutResult first = provider.execute(instruction(1));
        PayoutResult retry = provider.execute(instruction(2));

        assertThat(first.outcome()).isEqualTo(PayoutOutcome.FAILED);
        assertThat(first.failureCode()).isEqualTo("DEMO_PAYOUT_FAILED");
        assertThat(first.failureMessage()).isEqualTo("Demonstration payout failed on the first attempt");
        assertThat(first.providerReference()).isNull();
        assertThat(retry.outcome()).isEqualTo(PayoutOutcome.CONFIRMED);
        assertThat(retry.providerReference()).isEqualTo("DEMO-PAYOUT-41");
    }

    private static DemoPaymentProviderProperties properties(DemoPayoutBehavior behavior) {
        DemoPaymentProviderProperties properties = new DemoPaymentProviderProperties();
        properties.setEnabled(true);
        properties.setPayoutBehavior(behavior);
        return properties;
    }

    private static PayoutInstruction instruction(int attemptCount) {
        return new PayoutInstruction(
                41L,
                "PAYOUT-PAYMENT-INTENT-31",
                "DEMO-RECIPIENT-21",
                new BigDecimal("1250.00"),
                "INR",
                attemptCount
        );
    }
}

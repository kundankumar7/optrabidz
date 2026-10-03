package com.project.optrabidz.financial.infrastructure.provider.demo;

import com.project.optrabidz.financial.application.payout.PayoutInstruction;
import com.project.optrabidz.financial.application.payout.PayoutProvider;
import com.project.optrabidz.financial.application.payout.PayoutResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@ConditionalOnProperty(
        prefix = "optrabidz.financial.demo-provider",
        name = "enabled",
        havingValue = "true"
)
public class DemoPayoutProvider implements PayoutProvider {
    private final DemoPaymentProviderProperties properties;

    public DemoPayoutProvider(DemoPaymentProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public PayoutResult execute(PayoutInstruction instruction) {
        if (properties.getPayoutBehavior() == DemoPayoutBehavior.FAIL_FIRST_ATTEMPT
                && instruction.attemptCount() == 1) {
            return PayoutResult.failed(
                    "DEMO_PAYOUT_FAILED",
                    "Demonstration payout failed on the first attempt"
            );
        }
        return PayoutResult.confirmed("DEMO-PAYOUT-" + instruction.payoutTransferId());
    }
}

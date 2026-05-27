package com.project.optrabidz.governance.application.lifecycle;

import com.project.optrabidz.governance.application.port.FinanceLifecycleGovernancePort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PaymentIntentExpiryRule implements LifecycleRule {
    private static final String RULE_NAME = "payment-intent-expiry";

    private final ObjectProvider<FinanceLifecycleGovernancePort> financeLifecyclePort;
    private final GovernanceLifecycleProperties lifecycleProperties;

    public PaymentIntentExpiryRule(ObjectProvider<FinanceLifecycleGovernancePort> financeLifecyclePort,
                                   GovernanceLifecycleProperties lifecycleProperties) {
        this.financeLifecyclePort = financeLifecyclePort;
        this.lifecycleProperties = lifecycleProperties;
    }

    @Override
    public String ruleName() {
        return RULE_NAME;
    }

    @Override
    public LifecycleEnforcementResult enforce(Instant now) {
        FinanceLifecycleGovernancePort port = financeLifecyclePort.getIfAvailable();
        if (port == null) {
            return LifecycleEnforcementResult.skipped(RULE_NAME, "Finance lifecycle port is not implemented yet");
        }

        int changedCount = port.expirePendingPaymentIntents(now, lifecycleProperties.getExpiryBatchSize());
        return LifecycleEnforcementResult.changed(RULE_NAME, changedCount);
    }
}

package com.project.optrabidz.governance.application.lifecycle;

import com.project.optrabidz.governance.application.port.FinanceLifecycleGovernancePort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SettlementExpiryRule implements LifecycleRule {
    private static final String RULE_NAME = "settlement-expiry";

    private final ObjectProvider<FinanceLifecycleGovernancePort> financeLifecyclePort;
    private final GovernanceLifecycleProperties lifecycleProperties;

    public SettlementExpiryRule(ObjectProvider<FinanceLifecycleGovernancePort> financeLifecyclePort,
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

        int changedCount = port.expirePendingSettlements(now, lifecycleProperties.getExpiryBatchSize());
        return LifecycleEnforcementResult.changed(RULE_NAME, changedCount);
    }
}

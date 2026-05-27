package com.project.optrabidz.governance.application.lifecycle;

import com.project.optrabidz.governance.application.port.MarketplaceLifecycleGovernancePort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ListingExpiryRule implements LifecycleRule {
    private static final String RULE_NAME = "listing-expiry";

    private final ObjectProvider<MarketplaceLifecycleGovernancePort> marketplaceLifecyclePort;
    private final GovernanceLifecycleProperties lifecycleProperties;

    public ListingExpiryRule(ObjectProvider<MarketplaceLifecycleGovernancePort> marketplaceLifecyclePort,
                             GovernanceLifecycleProperties lifecycleProperties) {
        this.marketplaceLifecyclePort = marketplaceLifecyclePort;
        this.lifecycleProperties = lifecycleProperties;
    }

    @Override
    public String ruleName() {
        return RULE_NAME;
    }

    @Override
    public LifecycleEnforcementResult enforce(Instant now) {
        MarketplaceLifecycleGovernancePort port = marketplaceLifecyclePort.getIfAvailable();
        if (port == null) {
            return LifecycleEnforcementResult.skipped(RULE_NAME, "Marketplace lifecycle port is not implemented yet");
        }

        int changedCount = port.expireOpenListings(now, lifecycleProperties.getExpiryBatchSize());
        return LifecycleEnforcementResult.changed(RULE_NAME, changedCount);
    }
}

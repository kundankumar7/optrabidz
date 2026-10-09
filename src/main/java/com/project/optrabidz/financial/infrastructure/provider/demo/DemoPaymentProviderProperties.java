package com.project.optrabidz.financial.infrastructure.provider.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "optrabidz.financial.demo-provider")
public class DemoPaymentProviderProperties {
    private boolean enabled;
    private DemoPayoutBehavior payoutBehavior = DemoPayoutBehavior.SUCCESS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DemoPayoutBehavior getPayoutBehavior() {
        return payoutBehavior;
    }

    public void setPayoutBehavior(DemoPayoutBehavior payoutBehavior) {
        this.payoutBehavior = payoutBehavior;
    }
}

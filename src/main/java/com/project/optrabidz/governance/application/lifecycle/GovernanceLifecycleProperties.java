package com.project.optrabidz.governance.application.lifecycle;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "optrabidz.governance.lifecycle")
public class GovernanceLifecycleProperties {
    private int expiryBatchSize = 100;

    public int getExpiryBatchSize() {
        return Math.max(1, expiryBatchSize);
    }

    public void setExpiryBatchSize(int expiryBatchSize) {
        this.expiryBatchSize = expiryBatchSize;
    }
}

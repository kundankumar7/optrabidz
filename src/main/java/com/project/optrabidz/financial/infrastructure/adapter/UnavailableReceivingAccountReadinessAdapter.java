package com.project.optrabidz.financial.infrastructure.adapter;

import com.project.optrabidz.marketplace.application.port.ReceivingAccountReadinessPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "optrabidz.financial.demo-provider.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class UnavailableReceivingAccountReadinessAdapter implements ReceivingAccountReadinessPort {
    @Override
    public boolean requiresVerifiedBinding() {
        return false;
    }

    @Override
    public boolean hasVerifiedBinding(Long accountId) {
        return false;
    }
}

package com.project.optrabidz.financial.infrastructure.adapter;

import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.marketplace.application.port.ReceivingAccountReadinessPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@ConditionalOnProperty(name = "optrabidz.financial.demo-provider.enabled", havingValue = "true")
public class DemoReceivingAccountReadinessAdapter implements ReceivingAccountReadinessPort {
    private static final String DEMO_PROVIDER = "DEMO";

    private final PaymentAccountBindingRepository repository;

    public DemoReceivingAccountReadinessAdapter(PaymentAccountBindingRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean requiresVerifiedBinding() {
        return true;
    }

    @Override
    public boolean hasVerifiedBinding(Long accountId) {
        return repository.findVerifiedByAccountIdAndProviderCode(accountId, DEMO_PROVIDER).isPresent();
    }
}

package com.project.optrabidz.financial.infrastructure.provider.sandbox;

import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import com.project.optrabidz.financial.infrastructure.entity.PaymentProvider;
import com.project.optrabidz.financial.infrastructure.entity.PaymentProviderMethod;
import com.project.optrabidz.financial.infrastructure.entity.PaymentProviderMethodId;
import com.project.optrabidz.financial.infrastructure.repository.JpaPaymentProviderMethodRepository;
import com.project.optrabidz.financial.infrastructure.repository.JpaPaymentProviderRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "optrabidz.financial.sandbox-providers.enabled", havingValue = "true")
public class SandboxPaymentProviderInitializer implements ApplicationRunner {
    private static final String DEFAULT_CURRENCY = "INR";

    private final JpaPaymentProviderRepository paymentProviderRepository;
    private final JpaPaymentProviderMethodRepository paymentProviderMethodRepository;

    public SandboxPaymentProviderInitializer(JpaPaymentProviderRepository paymentProviderRepository,
                                             JpaPaymentProviderMethodRepository paymentProviderMethodRepository) {
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentProviderMethodRepository = paymentProviderMethodRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = Instant.now();
        ensureProvider(SandboxPaymentProviderCodes.UPI, "Sandbox UPI Payment", now);
        ensureProvider(SandboxPaymentProviderCodes.CARD, "Sandbox Card Payment", now);
        ensureMethod(SandboxPaymentProviderCodes.UPI, PaymentMethodType.UPI);
        ensureMethod(SandboxPaymentProviderCodes.CARD, PaymentMethodType.CARD);
    }

    private void ensureProvider(String providerCode, String displayName, Instant now) {
        PaymentProvider provider = paymentProviderRepository.findById(providerCode)
                .orElseGet(PaymentProvider::new);
        if (provider.getCreatedAt() == null) {
            provider.setCreatedAt(now);
        } else {
            provider.setUpdatedAt(now);
        }
        provider.setProviderCode(providerCode);
        provider.setDisplayName(displayName);
        provider.setEnabled(true);
        paymentProviderRepository.save(provider);
    }

    private void ensureMethod(String providerCode, PaymentMethodType methodType) {
        PaymentProviderMethodId id = new PaymentProviderMethodId(providerCode, methodType, DEFAULT_CURRENCY);
        PaymentProviderMethod method = paymentProviderMethodRepository.findById(id)
                .orElseGet(PaymentProviderMethod::new);
        method.setProviderCode(providerCode);
        method.setMethodType(methodType);
        method.setCurrencyCode(DEFAULT_CURRENCY);
        method.setEnabled(true);
        paymentProviderMethodRepository.save(method);
    }
}

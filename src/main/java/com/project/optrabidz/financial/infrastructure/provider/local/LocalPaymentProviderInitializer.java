package com.project.optrabidz.financial.infrastructure.provider.local;

import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import com.project.optrabidz.financial.infrastructure.entity.PaymentProvider;
import com.project.optrabidz.financial.infrastructure.entity.PaymentProviderMethod;
import com.project.optrabidz.financial.infrastructure.repository.JpaPaymentProviderMethodRepository;
import com.project.optrabidz.financial.infrastructure.repository.JpaPaymentProviderRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "optrabidz.financial.local-provider.enabled", havingValue = "true")
public class LocalPaymentProviderInitializer implements ApplicationRunner {
    private static final String LOCAL_PROVIDER_CODE = "LOCAL";
    private static final String DEFAULT_CURRENCY = "INR";

    private final JpaPaymentProviderRepository paymentProviderRepository;
    private final JpaPaymentProviderMethodRepository paymentProviderMethodRepository;

    public LocalPaymentProviderInitializer(JpaPaymentProviderRepository paymentProviderRepository,
                                           JpaPaymentProviderMethodRepository paymentProviderMethodRepository) {
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentProviderMethodRepository = paymentProviderMethodRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!paymentProviderRepository.existsById(LOCAL_PROVIDER_CODE)) {
            PaymentProvider provider = new PaymentProvider();
            provider.setProviderCode(LOCAL_PROVIDER_CODE);
            provider.setDisplayName("Local Test Payment");
            provider.setEnabled(true);
            provider.setCreatedAt(Instant.now());
            paymentProviderRepository.save(provider);
        }

        if (!paymentProviderMethodRepository.existsByProviderCodeAndMethodTypeAndCurrencyCodeAndEnabledTrue(
                LOCAL_PROVIDER_CODE,
                PaymentMethodType.OTHER,
                DEFAULT_CURRENCY
        )) {
            PaymentProviderMethod method = new PaymentProviderMethod();
            method.setProviderCode(LOCAL_PROVIDER_CODE);
            method.setMethodType(PaymentMethodType.OTHER);
            method.setCurrencyCode(DEFAULT_CURRENCY);
            method.setEnabled(true);
            paymentProviderMethodRepository.save(method);
        }
    }
}

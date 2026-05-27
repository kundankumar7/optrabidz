package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import com.project.optrabidz.financial.infrastructure.entity.PaymentProviderMethod;
import com.project.optrabidz.financial.infrastructure.entity.PaymentProviderMethodId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPaymentProviderMethodRepository extends JpaRepository<PaymentProviderMethod, PaymentProviderMethodId> {
    boolean existsByProviderCodeAndMethodTypeAndCurrencyCodeAndEnabledTrue(String providerCode,
                                                                           PaymentMethodType methodType,
                                                                           String currencyCode);
}

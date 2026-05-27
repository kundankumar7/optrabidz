package com.project.optrabidz.financial.application.strategy;

import com.project.optrabidz.financial.application.exception.UnsupportedPaymentMethodException;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentMethodStrategyRegistry {
    private final List<PaymentMethodStrategy> strategies;

    public PaymentMethodStrategyRegistry(List<PaymentMethodStrategy> strategies) {
        this.strategies = strategies;
    }

    public PaymentMethodStrategy resolve(String providerCode, PaymentMethodType methodType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(providerCode, methodType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedPaymentMethodException(
                        "Payment method is not supported for provider " + providerCode
                ));
    }
}

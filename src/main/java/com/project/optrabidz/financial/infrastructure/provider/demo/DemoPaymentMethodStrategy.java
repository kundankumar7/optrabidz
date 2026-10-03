package com.project.optrabidz.financial.infrastructure.provider.demo;

import com.project.optrabidz.financial.application.strategy.PaymentMethodStrategy;
import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("demo")
@ConditionalOnProperty(name = "optrabidz.financial.demo-provider.enabled", havingValue = "true")
public class DemoPaymentMethodStrategy implements PaymentMethodStrategy {
    public static final String PROVIDER_CODE = "DEMO";

    @Override
    public boolean supports(String providerCode, PaymentMethodType methodType) {
        return PROVIDER_CODE.equalsIgnoreCase(providerCode)
                && (methodType == PaymentMethodType.UPI || methodType == PaymentMethodType.CARD);
    }

    @Override
    public PaymentAttempt initiate(PaymentIntent paymentIntent,
                                   PaymentAttempt paymentAttempt,
                                   Instant now) {
        paymentAttempt.markInitiated(
                "DEMO-ORDER-" + paymentAttempt.getPaymentAttemptId(),
                "DEMO-CHECKOUT-" + paymentAttempt.getPaymentAttemptId(),
                "{\"environment\":\"DEMONSTRATION\"}",
                now
        );
        return paymentAttempt;
    }
}

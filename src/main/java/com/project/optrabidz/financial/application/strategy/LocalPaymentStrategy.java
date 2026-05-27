package com.project.optrabidz.financial.application.strategy;

import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "optrabidz.financial.local-provider.enabled", havingValue = "true")
public class LocalPaymentStrategy implements PaymentMethodStrategy {
    public static final String PROVIDER_CODE = "LOCAL";

    @Override
    public boolean supports(String providerCode, PaymentMethodType methodType) {
        return PROVIDER_CODE.equalsIgnoreCase(providerCode) && methodType == PaymentMethodType.OTHER;
    }

    @Override
    public PaymentAttempt initiate(PaymentIntent paymentIntent, PaymentAttempt paymentAttempt, Instant now) {
        String reference = "LOCAL-ATTEMPT-" + paymentAttempt.getPaymentAttemptId();
        String payload = """
                {"mode":"LOCAL","message":"Use /api/v1/payment-attempts/%d/actions/local-confirm to simulate success"}
                """.formatted(paymentAttempt.getPaymentAttemptId()).trim();
        paymentAttempt.markInitiated(reference, reference, payload, now);
        return paymentAttempt;
    }
}

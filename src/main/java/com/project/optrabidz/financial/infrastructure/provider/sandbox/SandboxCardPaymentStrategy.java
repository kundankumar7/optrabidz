package com.project.optrabidz.financial.infrastructure.provider.sandbox;

import com.project.optrabidz.financial.application.strategy.PaymentMethodStrategy;
import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "optrabidz.financial.sandbox-providers.enabled", havingValue = "true")
public class SandboxCardPaymentStrategy implements PaymentMethodStrategy {
    @Override
    public boolean supports(String providerCode, PaymentMethodType methodType) {
        return SandboxPaymentProviderCodes.CARD.equalsIgnoreCase(providerCode)
                && methodType == PaymentMethodType.CARD;
    }

    @Override
    public PaymentAttempt initiate(PaymentIntent paymentIntent, PaymentAttempt paymentAttempt, Instant now) {
        String orderId = "CARD-ORDER-" + paymentAttempt.getPaymentAttemptId();
        String referenceId = "CARD-REF-" + paymentAttempt.getPaymentAttemptId();
        String payload = """
                {"provider":"CARD","mode":"SANDBOX","paymentAttemptId":%d,"paymentIntentId":%d,"amount":"%s","currencyCode":"%s","checkoutUrl":"https://sandbox.payments.optrabidz.local/card-checkout/%d"}
                """.formatted(
                paymentAttempt.getPaymentAttemptId(),
                paymentIntent.getPaymentIntentId(),
                paymentIntent.getAmount().toPlainString(),
                paymentIntent.getCurrencyCode(),
                paymentAttempt.getPaymentAttemptId()
        ).trim();
        paymentAttempt.markInitiated(orderId, referenceId, payload, now);
        return paymentAttempt;
    }
}

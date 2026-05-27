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
public class SandboxUpiPaymentStrategy implements PaymentMethodStrategy {
    @Override
    public boolean supports(String providerCode, PaymentMethodType methodType) {
        return SandboxPaymentProviderCodes.UPI.equalsIgnoreCase(providerCode)
                && methodType == PaymentMethodType.UPI;
    }

    @Override
    public PaymentAttempt initiate(PaymentIntent paymentIntent, PaymentAttempt paymentAttempt, Instant now) {
        String orderId = "UPI-ORDER-" + paymentAttempt.getPaymentAttemptId();
        String referenceId = "UPI-REF-" + paymentAttempt.getPaymentAttemptId();
        String payload = """
                {"provider":"UPI","mode":"SANDBOX","paymentAttemptId":%d,"paymentIntentId":%d,"amount":"%s","currencyCode":"%s","collectUrl":"upi://pay?pa=optrabidz@upi&am=%s&cu=%s&tr=%s"}
                """.formatted(
                paymentAttempt.getPaymentAttemptId(),
                paymentIntent.getPaymentIntentId(),
                paymentIntent.getAmount().toPlainString(),
                paymentIntent.getCurrencyCode(),
                paymentIntent.getAmount().toPlainString(),
                paymentIntent.getCurrencyCode(),
                referenceId
        ).trim();
        paymentAttempt.markInitiated(orderId, referenceId, payload, now);
        return paymentAttempt;
    }
}

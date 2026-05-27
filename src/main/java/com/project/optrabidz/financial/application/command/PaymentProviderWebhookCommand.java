package com.project.optrabidz.financial.application.command;

import org.springframework.util.Assert;

import java.util.Map;

public record PaymentProviderWebhookCommand(
        String providerCode,
        PaymentProviderWebhookEventType eventType,
        Long paymentAttemptId,
        String providerPaymentId,
        String failureCode,
        String failureMessage,
        String providerEventId,
        String rawPayload,
        Map<String, String> headers
) {
    public PaymentProviderWebhookCommand {
        Assert.hasText(providerCode, "providerCode must not be blank");
        Assert.notNull(eventType, "eventType must not be null");
        Assert.notNull(paymentAttemptId, "paymentAttemptId must not be null");
        Assert.hasText(providerEventId, "providerEventId must not be blank");
        Assert.hasText(rawPayload, "rawPayload must not be blank");

        providerCode = providerCode.trim().toUpperCase();
        if (eventType == PaymentProviderWebhookEventType.PAYMENT_CONFIRMED) {
            Assert.hasText(providerPaymentId, "providerPaymentId must not be blank for confirmation webhook");
            providerPaymentId = providerPaymentId.trim();
        }
        if (eventType == PaymentProviderWebhookEventType.PAYMENT_FAILED) {
            failureCode = failureCode == null || failureCode.isBlank()
                    ? "PROVIDER_FAILURE"
                    : failureCode.trim().toUpperCase();
            failureMessage = failureMessage == null || failureMessage.isBlank()
                    ? "Provider reported payment failure"
                    : failureMessage.trim();
        }
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public PaymentProviderWebhookCommand(String providerCode,
                                         PaymentProviderWebhookEventType eventType,
                                         Long paymentAttemptId,
                                         String providerPaymentId,
                                         String providerEventId,
                                         String rawPayload,
                                         Map<String, String> headers) {
        this(
                providerCode,
                eventType,
                paymentAttemptId,
                providerPaymentId,
                null,
                null,
                providerEventId,
                rawPayload,
                headers
        );
    }
}

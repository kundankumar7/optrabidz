package com.project.optrabidz.financial.application.dto.request;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;

import java.util.Map;

public record PaymentProviderWebhookRequest(
        PaymentProviderWebhookEventType eventType,
        Long paymentAttemptId,
        String providerPaymentId,
        String failureCode,
        String failureMessage,
        String providerEventId
) {
    public PaymentProviderWebhookCommand toCommand(String providerCode,
                                                   String rawPayload,
                                                   Map<String, String> headers) {
        return new PaymentProviderWebhookCommand(
                providerCode,
                eventType,
                paymentAttemptId,
                providerPaymentId,
                failureCode,
                failureMessage,
                providerEventId,
                rawPayload,
                headers
        );
    }
}

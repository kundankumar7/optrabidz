package com.project.optrabidz.financial.application.replay;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;

import java.util.Objects;

public record PaymentWebhookReplayContent(
        int fingerprintVersion,
        String providerCode,
        String providerEventId,
        PaymentProviderWebhookEventType eventType,
        Long paymentAttemptId,
        String providerPaymentId,
        String providerFailureCode,
        String providerFailureMessage
) {
    public PaymentWebhookReplayContent {
        if (fingerprintVersion <= 0) {
            throw new IllegalArgumentException("fingerprintVersion must be positive");
        }
        Objects.requireNonNull(providerCode, "providerCode must not be null");
        Objects.requireNonNull(providerEventId, "providerEventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(paymentAttemptId, "paymentAttemptId must not be null");
    }
}

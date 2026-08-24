package com.project.optrabidz.financial.application.dto.request;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PaymentProviderWebhookRequest(
        @NotNull PaymentProviderWebhookEventType eventType,
        @NotNull @Positive Long paymentAttemptId,
        @Size(max = 128) String providerPaymentId,
        @Size(max = 64) String failureCode,
        @Size(max = 512) String failureMessage,
        @NotBlank @Size(max = 128) String providerEventId
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

package com.project.optrabidz.financial.application.dto.response;

import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;

import java.time.Instant;

public record PaymentAttemptResponse(
        Long paymentAttemptId,
        Long paymentIntentId,
        String providerCode,
        PaymentMethodType methodType,
        String providerOrderId,
        String providerPaymentId,
        String providerReferenceId,
        PaymentAttemptState attemptState,
        Instant createdAt,
        Instant initiatedAt,
        Instant confirmedAt,
        Instant failedAt,
        String failureCode,
        String failureMessage,
        String providerPayload
) {
}

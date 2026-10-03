package com.project.optrabidz.financial.application.dto.response;

import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentState;

import java.time.Instant;

public record DemoPaymentOutcomeResponse(
        Long paymentAttemptId,
        Long paymentIntentId,
        PaymentAttemptState attemptState,
        PaymentState paymentState,
        Instant processedAt
) {
}

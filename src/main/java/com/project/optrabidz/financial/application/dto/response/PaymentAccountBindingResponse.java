package com.project.optrabidz.financial.application.dto.response;

import com.project.optrabidz.financial.domain.model.PaymentAccountBindingStatus;

import java.time.Instant;

public record PaymentAccountBindingResponse(
        Long paymentAccountBindingId,
        String providerCode,
        PaymentAccountBindingStatus status,
        String maskedAccountLabel,
        String demoBankLabel,
        boolean ready,
        Instant createdAt,
        Instant verifiedAt,
        Instant deactivatedAt
) {
}

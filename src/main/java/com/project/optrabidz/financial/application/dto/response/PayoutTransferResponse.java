package com.project.optrabidz.financial.application.dto.response;

import com.project.optrabidz.financial.domain.model.PayoutTransferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PayoutTransferResponse(
        Long payoutTransferId,
        Long paymentIntentId,
        String providerCode,
        PayoutTransferStatus status,
        BigDecimal amount,
        String currencyCode,
        String maskedDestination,
        int attemptCount,
        Instant createdAt,
        Instant lastAttemptAt,
        Instant confirmedAt,
        Instant latestFailureAt,
        String latestFailureCode
) {
}

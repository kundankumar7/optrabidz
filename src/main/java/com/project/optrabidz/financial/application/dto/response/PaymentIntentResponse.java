package com.project.optrabidz.financial.application.dto.response;

import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentIntentResponse(
        Long paymentIntentId,
        PaymentPurpose paymentPurpose,
        Long settlementId,
        Long repaymentInstallmentId,
        Long payerAccountId,
        Long payeeAccountId,
        BigDecimal amount,
        String currencyCode,
        PaymentState paymentState,
        Instant createdAt,
        Instant expiresAt,
        Instant confirmedAt,
        Instant failedAt,
        Instant expiredAt,
        Instant cancelledAt,
        String failureCode,
        String failureMessage
) {
}

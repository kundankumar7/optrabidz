package com.project.optrabidz.financial.application.dto.response;

import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;

import java.math.BigDecimal;
import java.time.Instant;

public record RepaymentInstallmentResponse(
        Long repaymentInstallmentId,
        Long repaymentId,
        Integer installmentNumber,
        BigDecimal amount,
        String currencyCode,
        Instant dueAt,
        RepaymentInstallmentState installmentState,
        Instant paymentStartedAt,
        Instant paidAt,
        Instant failedAt,
        Instant overdueAt,
        Instant cancelledAt,
        String failureReason,
        Long confirmedPaymentIntentId,
        Instant createdAt,
        Instant updatedAt
) {
}

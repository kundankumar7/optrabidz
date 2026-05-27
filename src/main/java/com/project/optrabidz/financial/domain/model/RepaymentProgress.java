package com.project.optrabidz.financial.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record RepaymentProgress(
        Long agreementId,
        Long repaymentId,
        Long startupId,
        Long investorId,
        String currencyCode,
        long totalInstallments,
        long paidInstallments,
        long unpaidInstallments,
        long failedInstallments,
        long overdueInstallments,
        long cancelledInstallments,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        RepaymentState repaymentState,
        Long nextInstallmentId,
        Integer nextInstallmentNumber,
        Instant nextDueAt
) {
}

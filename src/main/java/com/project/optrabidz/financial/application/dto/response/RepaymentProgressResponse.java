package com.project.optrabidz.financial.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record RepaymentProgressResponse(
        Long agreementId,
        Long repaymentId,
        Long startupId,
        Long investorId,
        Integer totalInstallments,
        Integer paidInstallments,
        Integer unpaidInstallments,
        Integer failedInstallments,
        Integer overdueInstallments,
        Integer cancelledInstallments,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String currencyCode,
        com.project.optrabidz.financial.domain.model.RepaymentState repaymentState,
        DebtTermsResponse debtTerms,
        Long nextInstallmentId,
        Integer nextInstallmentNumber,
        Instant nextDueAt
) {
}

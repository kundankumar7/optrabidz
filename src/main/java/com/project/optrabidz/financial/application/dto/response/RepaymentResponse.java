package com.project.optrabidz.financial.application.dto.response;

import com.project.optrabidz.financial.domain.model.RepaymentState;

import java.math.BigDecimal;
import java.time.Instant;

public record RepaymentResponse(
        Long repaymentId,
        Long agreementId,
        Long startupId,
        Long investorId,
        BigDecimal totalRepayableAmount,
        String currencyCode,
        Integer totalInstallments,
        com.project.optrabidz.marketplace.domain.model.RepaymentPlanType repaymentPlanType,
        DebtTermsResponse debtTerms,
        RepaymentState repaymentState,
        Instant startedAt,
        Instant finalDueAt,
        Instant createdAt,
        Instant cancelledAt,
        Instant completedAt,
        Instant updatedAt
) {
}

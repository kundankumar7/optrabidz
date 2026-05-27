package com.project.optrabidz.marketplace.application.dto.response;

import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;

import java.math.BigDecimal;

public record BidDebtTermsResponse(
        BigDecimal proposedAmount,
        BigDecimal proposedInterestRate,
        Integer proposedTenureMonths,
        RepaymentPlanType repaymentPlanType,
        Integer oneTimeRepaymentDueAfterMonths
) {
}

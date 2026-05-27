package com.project.optrabidz.marketplace.application.dto.response;

import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;

import java.math.BigDecimal;

public record ListingDebtTermsResponse(
        BigDecimal requestedAmount,
        String currencyCode,
        BigDecimal minimumInterestRate,
        BigDecimal maximumInterestRate,
        Integer requestedTenureMonths,
        RepaymentPlanType repaymentPlanType,
        Integer oneTimeRepaymentDueAfterMonths
) {
}

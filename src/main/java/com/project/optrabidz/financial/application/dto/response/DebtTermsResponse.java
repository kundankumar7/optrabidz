package com.project.optrabidz.financial.application.dto.response;

import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;

import java.math.BigDecimal;

public record DebtTermsResponse(
        BigDecimal principalAmount,
        BigDecimal interestRate,
        Integer tenureMonths,
        RepaymentPlanType repaymentPlanType,
        Integer oneTimeRepaymentDueAfterMonths
) {
}

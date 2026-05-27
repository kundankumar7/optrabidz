package com.project.optrabidz.marketplace.application.dto.response;

import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;

import java.math.BigDecimal;

public record AgreementDebtTermsResponse(
        BigDecimal principalAmount,
        BigDecimal interestRate,
        Integer tenureMonths,
        RepaymentPlanType repaymentPlanType,
        Integer oneTimeRepaymentDueAfterMonths
) {
}

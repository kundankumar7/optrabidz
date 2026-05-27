package com.project.optrabidz.marketplace.application.dto.request;

import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BidDebtTermsRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal proposedAmount,
        @NotNull @DecimalMin(value = "0.0") BigDecimal proposedInterestRate,
        @NotNull @Positive Integer proposedTenureMonths,
        @NotNull RepaymentPlanType repaymentPlanType,
        @Positive Integer oneTimeRepaymentDueAfterMonths
) {
}

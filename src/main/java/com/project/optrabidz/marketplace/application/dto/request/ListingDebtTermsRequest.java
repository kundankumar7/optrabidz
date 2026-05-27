package com.project.optrabidz.marketplace.application.dto.request;

import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ListingDebtTermsRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal requestedAmount,
        @NotBlank String currencyCode,
        @DecimalMin(value = "0.0") BigDecimal minimumInterestRate,
        @DecimalMin(value = "0.0") BigDecimal maximumInterestRate,
        @Positive Integer requestedTenureMonths,
        @NotNull RepaymentPlanType repaymentPlanType,
        @Positive Integer oneTimeRepaymentDueAfterMonths
) {
}

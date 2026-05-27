package com.project.optrabidz.marketplace.domain.model;

import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

public class ListingDebtTerms {
    private Long listingDebtTermsId;
    private Long listingId;
    private BigDecimal requestedAmount;
    private String currencyCode;
    private BigDecimal minimumInterestRate;
    private BigDecimal maximumInterestRate;
    private Integer requestedTenureMonths;
    private RepaymentPlanType repaymentPlanType;
    private Integer oneTimeRepaymentDueAfterMonths;
    private Instant createdAt;
    private Instant updatedAt;

    public ListingDebtTerms(Long listingDebtTermsId,
                            Long listingId,
                            BigDecimal requestedAmount,
                            String currencyCode,
                            BigDecimal minimumInterestRate,
                            BigDecimal maximumInterestRate,
                            Integer requestedTenureMonths,
                            RepaymentPlanType repaymentPlanType,
                            Integer oneTimeRepaymentDueAfterMonths,
                            Instant createdAt,
                            Instant updatedAt) {
        this.listingDebtTermsId = listingDebtTermsId;
        this.listingId = listingId;
        update(requestedAmount, currencyCode, minimumInterestRate, maximumInterestRate,
                requestedTenureMonths, repaymentPlanType, oneTimeRepaymentDueAfterMonths, updatedAt);
        this.createdAt = createdAt;
    }

    public static ListingDebtTerms create(BigDecimal requestedAmount,
                                          String currencyCode,
                                          BigDecimal minimumInterestRate,
                                          BigDecimal maximumInterestRate,
                                          Integer requestedTenureMonths,
                                          RepaymentPlanType repaymentPlanType,
                                          Integer oneTimeRepaymentDueAfterMonths,
                                          Instant createdAt) {
        return new ListingDebtTerms(null, null, requestedAmount, currencyCode, minimumInterestRate,
                maximumInterestRate, requestedTenureMonths, repaymentPlanType,
                oneTimeRepaymentDueAfterMonths, createdAt, null);
    }

    public void update(BigDecimal requestedAmount,
                       String currencyCode,
                       BigDecimal minimumInterestRate,
                       BigDecimal maximumInterestRate,
                       Integer requestedTenureMonths,
                       RepaymentPlanType repaymentPlanType,
                       Integer oneTimeRepaymentDueAfterMonths,
                       Instant updatedAt) {
        Assert.notNull(requestedAmount, "requestedAmount must not be null");
        Assert.isTrue(requestedAmount.signum() > 0, "requestedAmount must be greater than zero");
        Assert.hasText(currencyCode, "currencyCode must not be blank");

        if (minimumInterestRate != null) {
            Assert.isTrue(minimumInterestRate.signum() >= 0, "minimumInterestRate must not be negative");
        }
        if (maximumInterestRate != null) {
            Assert.isTrue(maximumInterestRate.signum() >= 0, "maximumInterestRate must not be negative");
        }
        if (minimumInterestRate != null && maximumInterestRate != null) {
            Assert.isTrue(minimumInterestRate.compareTo(maximumInterestRate) <= 0,
                    "minimumInterestRate must be less than or equal to maximumInterestRate");
        }
        if (requestedTenureMonths != null) {
            Assert.isTrue(requestedTenureMonths > 0, "requestedTenureMonths must be greater than zero");
        }
        DebtRepaymentPlanRules.validate(
                repaymentPlanType,
                oneTimeRepaymentDueAfterMonths,
                requestedTenureMonths,
                "requestedTenureMonths"
        );

        this.requestedAmount = requestedAmount;
        this.currencyCode = currencyCode.trim().toUpperCase();
        this.minimumInterestRate = minimumInterestRate;
        this.maximumInterestRate = maximumInterestRate;
        this.requestedTenureMonths = requestedTenureMonths;
        this.repaymentPlanType = repaymentPlanType;
        this.oneTimeRepaymentDueAfterMonths = oneTimeRepaymentDueAfterMonths;
        this.updatedAt = updatedAt;
    }

    public Long getListingDebtTermsId() {
        return listingDebtTermsId;
    }

    public Long getListingId() {
        return listingId;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getMinimumInterestRate() {
        return minimumInterestRate;
    }

    public BigDecimal getMaximumInterestRate() {
        return maximumInterestRate;
    }

    public Integer getRequestedTenureMonths() {
        return requestedTenureMonths;
    }

    public RepaymentPlanType getRepaymentPlanType() {
        return repaymentPlanType;
    }

    public Integer getOneTimeRepaymentDueAfterMonths() {
        return oneTimeRepaymentDueAfterMonths;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

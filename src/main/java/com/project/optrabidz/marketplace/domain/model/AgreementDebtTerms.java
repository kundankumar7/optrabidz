package com.project.optrabidz.marketplace.domain.model;

import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

public class AgreementDebtTerms {
    private Long agreementDebtTermsId;
    private Long agreementId;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private RepaymentPlanType repaymentPlanType;
    private Integer oneTimeRepaymentDueAfterMonths;
    private Instant createdAt;

    public AgreementDebtTerms(Long agreementDebtTermsId,
                              Long agreementId,
                              BigDecimal principalAmount,
                              BigDecimal interestRate,
                              Integer tenureMonths,
                              RepaymentPlanType repaymentPlanType,
                              Integer oneTimeRepaymentDueAfterMonths,
                              Instant createdAt) {
        Assert.notNull(principalAmount, "principalAmount must not be null");
        Assert.isTrue(principalAmount.signum() > 0, "principalAmount must be greater than zero");
        Assert.notNull(interestRate, "interestRate must not be null");
        Assert.isTrue(interestRate.signum() >= 0, "interestRate must not be negative");
        Assert.notNull(tenureMonths, "tenureMonths must not be null");
        Assert.isTrue(tenureMonths > 0, "tenureMonths must be greater than zero");
        DebtRepaymentPlanRules.validate(
                repaymentPlanType,
                oneTimeRepaymentDueAfterMonths,
                tenureMonths,
                "tenureMonths"
        );

        this.agreementDebtTermsId = agreementDebtTermsId;
        this.agreementId = agreementId;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.tenureMonths = tenureMonths;
        this.repaymentPlanType = repaymentPlanType;
        this.oneTimeRepaymentDueAfterMonths = oneTimeRepaymentDueAfterMonths;
        this.createdAt = createdAt;
    }

    public static AgreementDebtTerms fromBidTerms(BidDebtTerms bidDebtTerms, Instant createdAt) {
        Assert.notNull(bidDebtTerms, "bidDebtTerms must not be null");
        return new AgreementDebtTerms(
                null,
                null,
                bidDebtTerms.getProposedAmount(),
                bidDebtTerms.getProposedInterestRate(),
                bidDebtTerms.getProposedTenureMonths(),
                bidDebtTerms.getRepaymentPlanType(),
                bidDebtTerms.getOneTimeRepaymentDueAfterMonths(),
                createdAt
        );
    }

    public Long getAgreementDebtTermsId() {
        return agreementDebtTermsId;
    }

    public Long getAgreementId() {
        return agreementId;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
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
}

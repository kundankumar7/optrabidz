package com.project.optrabidz.marketplace.domain.model;

import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

public class BidDebtTerms {
    private Long bidDebtTermsId;
    private Long bidId;
    private BigDecimal proposedAmount;
    private BigDecimal proposedInterestRate;
    private Integer proposedTenureMonths;
    private RepaymentPlanType repaymentPlanType;
    private Integer oneTimeRepaymentDueAfterMonths;
    private Instant createdAt;
    private Instant updatedAt;

    public BidDebtTerms(Long bidDebtTermsId,
                        Long bidId,
                        BigDecimal proposedAmount,
                        BigDecimal proposedInterestRate,
                        Integer proposedTenureMonths,
                        RepaymentPlanType repaymentPlanType,
                        Integer oneTimeRepaymentDueAfterMonths,
                        Instant createdAt,
                        Instant updatedAt) {
        this.bidDebtTermsId = bidDebtTermsId;
        this.bidId = bidId;
        update(proposedAmount, proposedInterestRate, proposedTenureMonths,
                repaymentPlanType, oneTimeRepaymentDueAfterMonths, updatedAt);
        this.createdAt = createdAt;
    }

    public static BidDebtTerms create(BigDecimal proposedAmount,
                                      BigDecimal proposedInterestRate,
                                      Integer proposedTenureMonths,
                                      RepaymentPlanType repaymentPlanType,
                                      Integer oneTimeRepaymentDueAfterMonths,
                                      Instant createdAt) {
        return new BidDebtTerms(null, null, proposedAmount, proposedInterestRate,
                proposedTenureMonths, repaymentPlanType, oneTimeRepaymentDueAfterMonths, createdAt, null);
    }

    public void update(BigDecimal proposedAmount,
                       BigDecimal proposedInterestRate,
                       Integer proposedTenureMonths,
                       RepaymentPlanType repaymentPlanType,
                       Integer oneTimeRepaymentDueAfterMonths,
                       Instant updatedAt) {
        Assert.notNull(proposedAmount, "proposedAmount must not be null");
        Assert.isTrue(proposedAmount.signum() > 0, "proposedAmount must be greater than zero");
        Assert.notNull(proposedInterestRate, "proposedInterestRate must not be null");
        Assert.isTrue(proposedInterestRate.signum() >= 0, "proposedInterestRate must not be negative");
        Assert.notNull(proposedTenureMonths, "proposedTenureMonths must not be null");
        Assert.isTrue(proposedTenureMonths > 0, "proposedTenureMonths must be greater than zero");
        DebtRepaymentPlanRules.validate(
                repaymentPlanType,
                oneTimeRepaymentDueAfterMonths,
                proposedTenureMonths,
                "proposedTenureMonths"
        );

        this.proposedAmount = proposedAmount;
        this.proposedInterestRate = proposedInterestRate;
        this.proposedTenureMonths = proposedTenureMonths;
        this.repaymentPlanType = repaymentPlanType;
        this.oneTimeRepaymentDueAfterMonths = oneTimeRepaymentDueAfterMonths;
        this.updatedAt = updatedAt;
    }

    public Long getBidDebtTermsId() {
        return bidDebtTermsId;
    }

    public Long getBidId() {
        return bidId;
    }

    public BigDecimal getProposedAmount() {
        return proposedAmount;
    }

    public BigDecimal getProposedInterestRate() {
        return proposedInterestRate;
    }

    public Integer getProposedTenureMonths() {
        return proposedTenureMonths;
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

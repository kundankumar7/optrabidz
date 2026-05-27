package com.project.optrabidz.financial.domain.model;

import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

public class Repayment {
    private final Long repaymentId;
    private final Long agreementId;
    private final Long startupId;
    private final Long investorId;
    private final BigDecimal totalRepayableAmount;
    private final String currencyCode;
    private final Integer totalInstallments;
    private final RepaymentPlanType repaymentPlanType;
    private RepaymentState repaymentState;
    private final Instant startedAt;
    private final Instant finalDueAt;
    private final Instant createdAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant updatedAt;

    private Repayment(Builder builder) {
        this.repaymentId = builder.repaymentId;
        this.agreementId = builder.agreementId;
        this.startupId = builder.startupId;
        this.investorId = builder.investorId;
        this.totalRepayableAmount = builder.totalRepayableAmount;
        this.currencyCode = builder.currencyCode;
        this.totalInstallments = builder.totalInstallments;
        this.repaymentPlanType = builder.repaymentPlanType;
        this.repaymentState = builder.repaymentState;
        this.startedAt = builder.startedAt;
        this.finalDueAt = builder.finalDueAt;
        this.createdAt = builder.createdAt;
        this.completedAt = builder.completedAt;
        this.cancelledAt = builder.cancelledAt;
        this.updatedAt = builder.updatedAt;
        validate();
    }

    public static Repayment create(Long agreementId,
                                   Long startupId,
                                   Long investorId,
                                   BigDecimal totalRepayableAmount,
                                   String currencyCode,
                                   Integer totalInstallments,
                                   RepaymentPlanType repaymentPlanType,
                                   Instant startedAt,
                                   Instant finalDueAt,
                                   Instant now) {
        return builder()
                .agreementId(agreementId)
                .startupId(startupId)
                .investorId(investorId)
                .totalRepayableAmount(totalRepayableAmount)
                .currencyCode(currencyCode)
                .totalInstallments(totalInstallments)
                .repaymentPlanType(repaymentPlanType)
                .repaymentState(RepaymentState.NOT_STARTED)
                .startedAt(startedAt)
                .finalDueAt(finalDueAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        Assert.notNull(agreementId, "agreementId must not be null");
        Assert.notNull(startupId, "startupId must not be null");
        Assert.notNull(investorId, "investorId must not be null");
        Assert.notNull(totalRepayableAmount, "totalRepayableAmount must not be null");
        Assert.isTrue(totalRepayableAmount.signum() > 0, "totalRepayableAmount must be greater than zero");
        Assert.hasText(currencyCode, "currencyCode must not be blank");
        Assert.notNull(totalInstallments, "totalInstallments must not be null");
        Assert.isTrue(totalInstallments > 0, "totalInstallments must be greater than zero");
        Assert.notNull(repaymentPlanType, "repaymentPlanType must not be null");
        Assert.notNull(repaymentState, "repaymentState must not be null");
        Assert.notNull(startedAt, "startedAt must not be null");
        Assert.notNull(finalDueAt, "finalDueAt must not be null");
        Assert.notNull(createdAt, "createdAt must not be null");
        Assert.notNull(updatedAt, "updatedAt must not be null");
        Assert.isTrue(!finalDueAt.isBefore(startedAt), "finalDueAt must not be before startedAt");
        Assert.isTrue(!updatedAt.isBefore(createdAt), "updatedAt must not be before createdAt");
    }

    public Long getRepaymentId() {
        return repaymentId;
    }

    public Long getAgreementId() {
        return agreementId;
    }

    public Long getStartupId() {
        return startupId;
    }

    public Long getInvestorId() {
        return investorId;
    }

    public BigDecimal getTotalRepayableAmount() {
        return totalRepayableAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public Integer getTotalInstallments() {
        return totalInstallments;
    }

    public RepaymentPlanType getRepaymentPlanType() {
        return repaymentPlanType;
    }

    public RepaymentState getRepaymentState() {
        return repaymentState;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinalDueAt() {
        return finalDueAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public static final class Builder {
        private Long repaymentId;
        private Long agreementId;
        private Long startupId;
        private Long investorId;
        private BigDecimal totalRepayableAmount;
        private String currencyCode;
        private Integer totalInstallments;
        private RepaymentPlanType repaymentPlanType;
        private RepaymentState repaymentState;
        private Instant startedAt;
        private Instant finalDueAt;
        private Instant createdAt;
        private Instant completedAt;
        private Instant cancelledAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder repaymentId(Long repaymentId) {
            this.repaymentId = repaymentId;
            return this;
        }

        public Builder agreementId(Long agreementId) {
            this.agreementId = agreementId;
            return this;
        }

        public Builder startupId(Long startupId) {
            this.startupId = startupId;
            return this;
        }

        public Builder investorId(Long investorId) {
            this.investorId = investorId;
            return this;
        }

        public Builder totalRepayableAmount(BigDecimal totalRepayableAmount) {
            this.totalRepayableAmount = totalRepayableAmount;
            return this;
        }

        public Builder currencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }

        public Builder totalInstallments(Integer totalInstallments) {
            this.totalInstallments = totalInstallments;
            return this;
        }

        public Builder repaymentPlanType(RepaymentPlanType repaymentPlanType) {
            this.repaymentPlanType = repaymentPlanType;
            return this;
        }

        public Builder repaymentState(RepaymentState repaymentState) {
            this.repaymentState = repaymentState;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder finalDueAt(Instant finalDueAt) {
            this.finalDueAt = finalDueAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder cancelledAt(Instant cancelledAt) {
            this.cancelledAt = cancelledAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Repayment build() {
            return new Repayment(this);
        }
    }
}

package com.project.optrabidz.financial.domain.model;

import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

public class RepaymentInstallment {
    private final Long repaymentInstallmentId;
    private final Long repaymentId;
    private final Integer installmentNumber;
    private RepaymentInstallmentState installmentState;
    private final BigDecimal amount;
    private final String currencyCode;
    private final Instant dueAt;
    private Instant paymentStartedAt;
    private Instant paidAt;
    private Instant failedAt;
    private Instant overdueAt;
    private Instant cancelledAt;
    private String failureReason;
    private Long confirmedPaymentIntentId;
    private final Instant createdAt;
    private Instant updatedAt;

    private RepaymentInstallment(Builder builder) {
        this.repaymentInstallmentId = builder.repaymentInstallmentId;
        this.repaymentId = builder.repaymentId;
        this.installmentNumber = builder.installmentNumber;
        this.installmentState = builder.installmentState;
        this.amount = builder.amount;
        this.currencyCode = builder.currencyCode;
        this.dueAt = builder.dueAt;
        this.paymentStartedAt = builder.paymentStartedAt;
        this.paidAt = builder.paidAt;
        this.failedAt = builder.failedAt;
        this.overdueAt = builder.overdueAt;
        this.cancelledAt = builder.cancelledAt;
        this.failureReason = builder.failureReason;
        this.confirmedPaymentIntentId = builder.confirmedPaymentIntentId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        validate();
    }

    public static RepaymentInstallment create(Long repaymentId,
                                              Integer installmentNumber,
                                              BigDecimal amount,
                                              String currencyCode,
                                              Instant dueAt,
                                              Instant now) {
        return builder()
                .repaymentId(repaymentId)
                .installmentNumber(installmentNumber)
                .installmentState(RepaymentInstallmentState.NOT_STARTED)
                .amount(amount)
                .currencyCode(currencyCode)
                .dueAt(dueAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        Assert.notNull(repaymentId, "repaymentId must not be null");
        Assert.notNull(installmentNumber, "installmentNumber must not be null");
        Assert.isTrue(installmentNumber > 0, "installmentNumber must be greater than zero");
        Assert.notNull(installmentState, "installmentState must not be null");
        Assert.notNull(amount, "amount must not be null");
        Assert.isTrue(amount.signum() > 0, "amount must be greater than zero");
        Assert.hasText(currencyCode, "currencyCode must not be blank");
        Assert.notNull(dueAt, "dueAt must not be null");
        Assert.notNull(createdAt, "createdAt must not be null");
        Assert.notNull(updatedAt, "updatedAt must not be null");
        Assert.isTrue(!updatedAt.isBefore(createdAt), "updatedAt must not be before createdAt");
    }

    public Long getRepaymentInstallmentId() {
        return repaymentInstallmentId;
    }

    public Long getRepaymentId() {
        return repaymentId;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public RepaymentInstallmentState getInstallmentState() {
        return installmentState;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getPaymentStartedAt() {
        return paymentStartedAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public Instant getOverdueAt() {
        return overdueAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Long getConfirmedPaymentIntentId() {
        return confirmedPaymentIntentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public static final class Builder {
        private Long repaymentInstallmentId;
        private Long repaymentId;
        private Integer installmentNumber;
        private RepaymentInstallmentState installmentState;
        private BigDecimal amount;
        private String currencyCode;
        private Instant dueAt;
        private Instant paymentStartedAt;
        private Instant paidAt;
        private Instant failedAt;
        private Instant overdueAt;
        private Instant cancelledAt;
        private String failureReason;
        private Long confirmedPaymentIntentId;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder repaymentInstallmentId(Long repaymentInstallmentId) {
            this.repaymentInstallmentId = repaymentInstallmentId;
            return this;
        }

        public Builder repaymentId(Long repaymentId) {
            this.repaymentId = repaymentId;
            return this;
        }

        public Builder installmentNumber(Integer installmentNumber) {
            this.installmentNumber = installmentNumber;
            return this;
        }

        public Builder installmentState(RepaymentInstallmentState installmentState) {
            this.installmentState = installmentState;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }

        public Builder dueAt(Instant dueAt) {
            this.dueAt = dueAt;
            return this;
        }

        public Builder paymentStartedAt(Instant paymentStartedAt) {
            this.paymentStartedAt = paymentStartedAt;
            return this;
        }

        public Builder paidAt(Instant paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public Builder failedAt(Instant failedAt) {
            this.failedAt = failedAt;
            return this;
        }

        public Builder overdueAt(Instant overdueAt) {
            this.overdueAt = overdueAt;
            return this;
        }

        public Builder cancelledAt(Instant cancelledAt) {
            this.cancelledAt = cancelledAt;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder confirmedPaymentIntentId(Long confirmedPaymentIntentId) {
            this.confirmedPaymentIntentId = confirmedPaymentIntentId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public RepaymentInstallment build() {
            return new RepaymentInstallment(this);
        }
    }
}

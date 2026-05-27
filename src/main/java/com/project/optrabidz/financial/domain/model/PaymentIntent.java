package com.project.optrabidz.financial.domain.model;

import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentIntent {
    private final Long paymentIntentId;
    private final PaymentPurpose paymentPurpose;
    private final Long settlementId;
    private final Long repaymentInstallmentId;
    private final Long payerAccountId;
    private final Long payeeAccountId;
    private final BigDecimal amount;
    private final String currencyCode;
    private PaymentState paymentState;
    private final String idempotencyKey;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant confirmedAt;
    private Instant failedAt;
    private Instant expiredAt;
    private Instant cancelledAt;
    private String failureCode;
    private String failureMessage;

    private PaymentIntent(Builder builder) {
        this.paymentIntentId = builder.paymentIntentId;
        this.paymentPurpose = builder.paymentPurpose;
        this.settlementId = builder.settlementId;
        this.repaymentInstallmentId = builder.repaymentInstallmentId;
        this.payerAccountId = builder.payerAccountId;
        this.payeeAccountId = builder.payeeAccountId;
        this.amount = builder.amount;
        this.currencyCode = builder.currencyCode;
        this.paymentState = builder.paymentState;
        this.idempotencyKey = builder.idempotencyKey;
        this.createdAt = builder.createdAt;
        this.expiresAt = builder.expiresAt;
        this.confirmedAt = builder.confirmedAt;
        this.failedAt = builder.failedAt;
        this.expiredAt = builder.expiredAt;
        this.cancelledAt = builder.cancelledAt;
        this.failureCode = builder.failureCode;
        this.failureMessage = builder.failureMessage;
        validate();
    }

    public static PaymentIntent forSettlement(Long settlementId,
                                              Long payerAccountId,
                                              Long payeeAccountId,
                                              BigDecimal amount,
                                              String currencyCode,
                                              String idempotencyKey,
                                              Instant createdAt,
                                              Instant expiresAt) {
        return builder()
                .paymentPurpose(PaymentPurpose.SETTLEMENT)
                .settlementId(settlementId)
                .payerAccountId(payerAccountId)
                .payeeAccountId(payeeAccountId)
                .amount(amount)
                .currencyCode(currencyCode)
                .paymentState(PaymentState.CREATED)
                .idempotencyKey(idempotencyKey)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
    }

    public static PaymentIntent forRepaymentInstallment(Long repaymentInstallmentId,
                                             Long payerAccountId,
                                             Long payeeAccountId,
                                             BigDecimal amount,
                                             String currencyCode,
                                             String idempotencyKey,
                                             Instant createdAt,
                                             Instant expiresAt) {
        return builder()
                .paymentPurpose(PaymentPurpose.REPAYMENT)
                .repaymentInstallmentId(repaymentInstallmentId)
                .payerAccountId(payerAccountId)
                .payeeAccountId(payeeAccountId)
                .amount(amount)
                .currencyCode(currencyCode)
                .paymentState(PaymentState.CREATED)
                .idempotencyKey(idempotencyKey)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void markPending() {
        if (paymentState != PaymentState.CREATED && paymentState != PaymentState.PAYMENT_PENDING) {
            throw new IllegalStateException("Only created or pending payment intent can be marked pending");
        }
        this.paymentState = PaymentState.PAYMENT_PENDING;
    }

    public void markConfirmed(Instant now) {
        if (paymentState != PaymentState.CREATED && paymentState != PaymentState.PAYMENT_PENDING) {
            throw new IllegalStateException("Only active payment intent can be confirmed");
        }
        this.paymentState = PaymentState.PAYMENT_CONFIRMED;
        this.confirmedAt = now;
    }

    public void markFailed(String failureCode, String failureMessage, Instant now) {
        if (paymentState != PaymentState.CREATED && paymentState != PaymentState.PAYMENT_PENDING) {
            throw new IllegalStateException("Only active payment intent can be failed");
        }
        this.paymentState = PaymentState.PAYMENT_FAILED;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.failedAt = now;
    }

    public boolean expireIfEligible(Instant now) {
        if ((paymentState != PaymentState.CREATED && paymentState != PaymentState.PAYMENT_PENDING) || expiresAt.isAfter(now)) {
            return false;
        }
        this.paymentState = PaymentState.PAYMENT_EXPIRED;
        this.expiredAt = now;
        return true;
    }

    private void validate() {
        Assert.notNull(paymentPurpose, "paymentPurpose must not be null");
        Assert.notNull(payerAccountId, "payerAccountId must not be null");
        Assert.notNull(payeeAccountId, "payeeAccountId must not be null");
        Assert.isTrue(!payerAccountId.equals(payeeAccountId), "payerAccountId and payeeAccountId must differ");
        Assert.notNull(amount, "amount must not be null");
        Assert.isTrue(amount.signum() > 0, "amount must be greater than zero");
        Assert.hasText(currencyCode, "currencyCode must not be blank");
        Assert.notNull(paymentState, "paymentState must not be null");
        Assert.hasText(idempotencyKey, "idempotencyKey must not be blank");
        Assert.notNull(createdAt, "createdAt must not be null");
        Assert.notNull(expiresAt, "expiresAt must not be null");
        Assert.isTrue(expiresAt.isAfter(createdAt), "expiresAt must be after createdAt");
        if (paymentPurpose == PaymentPurpose.SETTLEMENT) {
            Assert.notNull(settlementId, "settlementId must not be null for settlement payment");
            Assert.isNull(repaymentInstallmentId, "repaymentInstallmentId must be null for settlement payment");
        }
        if (paymentPurpose == PaymentPurpose.REPAYMENT) {
            Assert.notNull(repaymentInstallmentId, "repaymentInstallmentId must not be null for repayment payment");
            Assert.isNull(settlementId, "settlementId must be null for repayment payment");
        }
    }

    public Long getPaymentIntentId() {
        return paymentIntentId;
    }

    public PaymentPurpose getPaymentPurpose() {
        return paymentPurpose;
    }

    public Long getSettlementId() {
        return settlementId;
    }

    public Long getRepaymentInstallmentId() {
        return repaymentInstallmentId;
    }

    public Long getPayerAccountId() {
        return payerAccountId;
    }

    public Long getPayeeAccountId() {
        return payeeAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public PaymentState getPaymentState() {
        return paymentState;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public static final class Builder {
        private Long paymentIntentId;
        private PaymentPurpose paymentPurpose;
        private Long settlementId;
        private Long repaymentInstallmentId;
        private Long payerAccountId;
        private Long payeeAccountId;
        private BigDecimal amount;
        private String currencyCode;
        private PaymentState paymentState;
        private String idempotencyKey;
        private Instant createdAt;
        private Instant expiresAt;
        private Instant confirmedAt;
        private Instant failedAt;
        private Instant expiredAt;
        private Instant cancelledAt;
        private String failureCode;
        private String failureMessage;

        private Builder() {
        }

        public Builder paymentIntentId(Long paymentIntentId) {
            this.paymentIntentId = paymentIntentId;
            return this;
        }

        public Builder paymentPurpose(PaymentPurpose paymentPurpose) {
            this.paymentPurpose = paymentPurpose;
            return this;
        }

        public Builder settlementId(Long settlementId) {
            this.settlementId = settlementId;
            return this;
        }

        public Builder repaymentInstallmentId(Long repaymentInstallmentId) {
            this.repaymentInstallmentId = repaymentInstallmentId;
            return this;
        }

        public Builder payerAccountId(Long payerAccountId) {
            this.payerAccountId = payerAccountId;
            return this;
        }

        public Builder payeeAccountId(Long payeeAccountId) {
            this.payeeAccountId = payeeAccountId;
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

        public Builder paymentState(PaymentState paymentState) {
            this.paymentState = paymentState;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder confirmedAt(Instant confirmedAt) {
            this.confirmedAt = confirmedAt;
            return this;
        }

        public Builder failedAt(Instant failedAt) {
            this.failedAt = failedAt;
            return this;
        }

        public Builder expiredAt(Instant expiredAt) {
            this.expiredAt = expiredAt;
            return this;
        }

        public Builder cancelledAt(Instant cancelledAt) {
            this.cancelledAt = cancelledAt;
            return this;
        }

        public Builder failureCode(String failureCode) {
            this.failureCode = failureCode;
            return this;
        }

        public Builder failureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
            return this;
        }

        public PaymentIntent build() {
            return new PaymentIntent(this);
        }
    }
}

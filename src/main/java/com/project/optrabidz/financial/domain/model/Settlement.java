package com.project.optrabidz.financial.domain.model;

import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

public class Settlement {
    private final Long settlementId;
    private final Long agreementId;
    private final Long startupId;
    private final Long investorId;
    private final BigDecimal amount;
    private final String currencyCode;
    private SettlementState settlementState;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant confirmedAt;
    private Instant failedAt;
    private Instant expiredAt;
    private Instant cancelledAt;
    private String failureReason;
    private Long confirmedPaymentIntentId;
    private final String pspReferenceId;

    private Settlement(Builder builder) {
        this.settlementId = builder.settlementId;
        this.agreementId = builder.agreementId;
        this.startupId = builder.startupId;
        this.investorId = builder.investorId;
        this.amount = builder.amount;
        this.currencyCode = builder.currencyCode;
        this.settlementState = builder.settlementState;
        this.createdAt = builder.createdAt;
        this.expiresAt = builder.expiresAt;
        this.confirmedAt = builder.confirmedAt;
        this.failedAt = builder.failedAt;
        this.expiredAt = builder.expiredAt;
        this.cancelledAt = builder.cancelledAt;
        this.failureReason = builder.failureReason;
        this.confirmedPaymentIntentId = builder.confirmedPaymentIntentId;
        this.pspReferenceId = builder.pspReferenceId;
        validate();
    }

    public static Settlement create(Long agreementId,
                                    Long startupId,
                                    Long investorId,
                                    BigDecimal amount,
                                    String currencyCode,
                                    Instant createdAt,
                                    Instant expiresAt) {
        return builder()
                .agreementId(agreementId)
                .startupId(startupId)
                .investorId(investorId)
                .amount(amount)
                .currencyCode(currencyCode)
                .settlementState(SettlementState.SETTLEMENT_PENDING)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void markConfirmed(Long paymentIntentId, Instant now) {
        ensurePending("Only pending settlement can be confirmed");
        Assert.notNull(paymentIntentId, "paymentIntentId must not be null");
        this.settlementState = SettlementState.SETTLEMENT_CONFIRMED;
        this.confirmedPaymentIntentId = paymentIntentId;
        this.confirmedAt = now;
    }

    public void markFailed(String reason, Instant now) {
        ensurePending("Only pending settlement can be failed");
        this.settlementState = SettlementState.SETTLEMENT_FAILED;
        this.failureReason = reason;
        this.failedAt = now;
    }

    public boolean expireIfEligible(Instant now) {
        if (settlementState != SettlementState.SETTLEMENT_PENDING || expiresAt.isAfter(now)) {
            return false;
        }
        this.settlementState = SettlementState.SETTLEMENT_EXPIRED;
        this.expiredAt = now;
        return true;
    }

    public void cancel(Instant now) {
        ensurePending("Only pending settlement can be cancelled");
        this.settlementState = SettlementState.SETTLEMENT_CANCELLED;
        this.cancelledAt = now;
    }

    private void validate() {
        Assert.notNull(agreementId, "agreementId must not be null");
        Assert.notNull(startupId, "startupId must not be null");
        Assert.notNull(investorId, "investorId must not be null");
        Assert.notNull(amount, "amount must not be null");
        Assert.isTrue(amount.signum() > 0, "amount must be greater than zero");
        Assert.hasText(currencyCode, "currencyCode must not be blank");
        Assert.notNull(settlementState, "settlementState must not be null");
        Assert.notNull(createdAt, "createdAt must not be null");
        Assert.notNull(expiresAt, "expiresAt must not be null");
        Assert.isTrue(expiresAt.isAfter(createdAt), "expiresAt must be after createdAt");
    }

    private void ensurePending(String message) {
        if (settlementState != SettlementState.SETTLEMENT_PENDING) {
            throw new IllegalStateException(message);
        }
    }

    public Long getSettlementId() {
        return settlementId;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public SettlementState getSettlementState() {
        return settlementState;
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

    public String getFailureReason() {
        return failureReason;
    }

    public Long getConfirmedPaymentIntentId() {
        return confirmedPaymentIntentId;
    }

    public String getPspReferenceId() {
        return pspReferenceId;
    }

    public static final class Builder {
        private Long settlementId;
        private Long agreementId;
        private Long startupId;
        private Long investorId;
        private BigDecimal amount;
        private String currencyCode;
        private SettlementState settlementState;
        private Instant createdAt;
        private Instant expiresAt;
        private Instant confirmedAt;
        private Instant failedAt;
        private Instant expiredAt;
        private Instant cancelledAt;
        private String failureReason;
        private Long confirmedPaymentIntentId;
        private String pspReferenceId;

        private Builder() {
        }

        public Builder settlementId(Long settlementId) {
            this.settlementId = settlementId;
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

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }

        public Builder settlementState(SettlementState settlementState) {
            this.settlementState = settlementState;
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

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder confirmedPaymentIntentId(Long confirmedPaymentIntentId) {
            this.confirmedPaymentIntentId = confirmedPaymentIntentId;
            return this;
        }

        public Builder pspReferenceId(String pspReferenceId) {
            this.pspReferenceId = pspReferenceId;
            return this;
        }

        public Settlement build() {
            return new Settlement(this);
        }
    }
}

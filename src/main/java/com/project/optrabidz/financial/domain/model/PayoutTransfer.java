package com.project.optrabidz.financial.domain.model;

import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;

public class PayoutTransfer {
    private final Long payoutTransferId;
    private final Long paymentIntentId;
    private final Long paymentAccountBindingId;
    private final String providerCode;
    private String providerTransferReference;
    private final String idempotencyKey;
    private final BigDecimal amount;
    private final String currencyCode;
    private PayoutTransferStatus transferStatus;
    private int attemptCount;
    private final Instant createdAt;
    private Instant lastAttemptAt;
    private Instant confirmedAt;
    private Instant latestFailureAt;
    private String latestFailureCode;
    private String latestFailureMessage;
    private Instant updatedAt;

    private PayoutTransfer(Builder builder) {
        this.payoutTransferId = builder.payoutTransferId;
        this.paymentIntentId = builder.paymentIntentId;
        this.paymentAccountBindingId = builder.paymentAccountBindingId;
        this.providerCode = builder.providerCode;
        this.providerTransferReference = builder.providerTransferReference;
        this.idempotencyKey = builder.idempotencyKey;
        this.amount = builder.amount;
        this.currencyCode = builder.currencyCode;
        this.transferStatus = builder.transferStatus;
        this.attemptCount = builder.attemptCount;
        this.createdAt = builder.createdAt;
        this.lastAttemptAt = builder.lastAttemptAt;
        this.confirmedAt = builder.confirmedAt;
        this.latestFailureAt = builder.latestFailureAt;
        this.latestFailureCode = builder.latestFailureCode;
        this.latestFailureMessage = builder.latestFailureMessage;
        this.updatedAt = builder.updatedAt;
        validate();
    }

    public static PayoutTransfer create(Long paymentIntentId,
                                        Long paymentAccountBindingId,
                                        String providerCode,
                                        String idempotencyKey,
                                        BigDecimal amount,
                                        String currencyCode,
                                        Instant createdAt) {
        return builder()
                .paymentIntentId(paymentIntentId)
                .paymentAccountBindingId(paymentAccountBindingId)
                .providerCode(providerCode)
                .idempotencyKey(idempotencyKey)
                .amount(amount)
                .currencyCode(currencyCode)
                .transferStatus(PayoutTransferStatus.PENDING)
                .attemptCount(0)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public void beginInitialAttempt(Instant now) {
        ensureStatus(PayoutTransferStatus.PENDING, "Initial payout attempt requires pending state");
        beginAttempt(now);
    }

    public void beginRetry(Instant now) {
        ensureStatus(PayoutTransferStatus.FAILED, "Payout retry requires failed state");
        beginAttempt(now);
    }

    public void markConfirmed(String providerReference, Instant now) {
        ensureStatus(PayoutTransferStatus.PROCESSING, "Only processing payout can be confirmed");
        Assert.hasText(providerReference, "providerReference must not be blank");
        requireAttemptTime(now, "confirmedAt");
        transferStatus = PayoutTransferStatus.CONFIRMED;
        providerTransferReference = providerReference;
        confirmedAt = now;
        updatedAt = now;
    }

    public void markFailed(String failureCode, String failureMessage, Instant now) {
        ensureStatus(PayoutTransferStatus.PROCESSING, "Only processing payout can fail");
        Assert.hasText(failureCode, "failureCode must not be blank");
        requireAttemptTime(now, "latestFailureAt");
        transferStatus = PayoutTransferStatus.FAILED;
        latestFailureAt = now;
        latestFailureCode = failureCode;
        latestFailureMessage = failureMessage;
        updatedAt = now;
    }

    private void beginAttempt(Instant now) {
        Assert.notNull(now, "lastAttemptAt must not be null");
        Assert.isTrue(!now.isBefore(createdAt), "lastAttemptAt must not be before createdAt");
        Assert.isTrue(!now.isBefore(updatedAt), "lastAttemptAt must not be before updatedAt");
        transferStatus = PayoutTransferStatus.PROCESSING;
        attemptCount++;
        lastAttemptAt = now;
        updatedAt = now;
    }

    private void requireAttemptTime(Instant now, String name) {
        Assert.notNull(now, name + " must not be null");
        Assert.isTrue(lastAttemptAt != null && !now.isBefore(lastAttemptAt),
                name + " must not be before lastAttemptAt");
        Assert.isTrue(!now.isBefore(updatedAt), name + " must not be before updatedAt");
    }

    private void ensureStatus(PayoutTransferStatus expected, String message) {
        if (transferStatus != expected) {
            throw new IllegalStateException(message);
        }
    }

    private void validate() {
        Assert.notNull(paymentIntentId, "paymentIntentId must not be null");
        Assert.notNull(paymentAccountBindingId, "paymentAccountBindingId must not be null");
        Assert.hasText(providerCode, "providerCode must not be blank");
        Assert.hasText(idempotencyKey, "idempotencyKey must not be blank");
        Assert.notNull(amount, "amount must not be null");
        Assert.isTrue(amount.signum() > 0, "amount must be greater than zero");
        Assert.hasText(currencyCode, "currencyCode must not be blank");
        Assert.notNull(transferStatus, "transferStatus must not be null");
        Assert.isTrue(attemptCount >= 0, "attemptCount must not be negative");
        Assert.notNull(createdAt, "createdAt must not be null");
        Assert.notNull(updatedAt, "updatedAt must not be null");
        Assert.isTrue(!updatedAt.isBefore(createdAt), "updatedAt must not be before createdAt");
        assertNotAfterUpdatedAt(lastAttemptAt, "lastAttemptAt");
        assertNotAfterUpdatedAt(confirmedAt, "confirmedAt");
        assertNotAfterUpdatedAt(latestFailureAt, "latestFailureAt");
        Assert.isTrue(lastAttemptAt == null || confirmedAt == null || !confirmedAt.isBefore(lastAttemptAt),
                "confirmedAt must not be before lastAttemptAt");
        Assert.isTrue(transferStatus != PayoutTransferStatus.FAILED || lastAttemptAt == null
                        || latestFailureAt == null || !latestFailureAt.isBefore(lastAttemptAt),
                "Failed transfer latestFailureAt must not be before lastAttemptAt");
    }

    private void assertNotAfterUpdatedAt(Instant eventAt, String name) {
        Assert.isTrue(eventAt == null || !eventAt.isAfter(updatedAt), name + " must not be after updatedAt");
    }

    public Long getPayoutTransferId() { return payoutTransferId; }
    public Long getPaymentIntentId() { return paymentIntentId; }
    public Long getPaymentAccountBindingId() { return paymentAccountBindingId; }
    public String getProviderCode() { return providerCode; }
    public String getProviderTransferReference() { return providerTransferReference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrencyCode() { return currencyCode; }
    public PayoutTransferStatus getTransferStatus() { return transferStatus; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getLatestFailureAt() { return latestFailureAt; }
    public String getLatestFailureCode() { return latestFailureCode; }
    public String getLatestFailureMessage() { return latestFailureMessage; }
    public Instant getUpdatedAt() { return updatedAt; }

    public static final class Builder {
        private Long payoutTransferId;
        private Long paymentIntentId;
        private Long paymentAccountBindingId;
        private String providerCode;
        private String providerTransferReference;
        private String idempotencyKey;
        private BigDecimal amount;
        private String currencyCode;
        private PayoutTransferStatus transferStatus;
        private int attemptCount;
        private Instant createdAt;
        private Instant lastAttemptAt;
        private Instant confirmedAt;
        private Instant latestFailureAt;
        private String latestFailureCode;
        private String latestFailureMessage;
        private Instant updatedAt;

        private Builder() {}

        public Builder payoutTransferId(Long value) { this.payoutTransferId = value; return this; }
        public Builder paymentIntentId(Long value) { this.paymentIntentId = value; return this; }
        public Builder paymentAccountBindingId(Long value) { this.paymentAccountBindingId = value; return this; }
        public Builder providerCode(String value) { this.providerCode = value; return this; }
        public Builder providerTransferReference(String value) { this.providerTransferReference = value; return this; }
        public Builder idempotencyKey(String value) { this.idempotencyKey = value; return this; }
        public Builder amount(BigDecimal value) { this.amount = value; return this; }
        public Builder currencyCode(String value) { this.currencyCode = value; return this; }
        public Builder transferStatus(PayoutTransferStatus value) { this.transferStatus = value; return this; }
        public Builder attemptCount(int value) { this.attemptCount = value; return this; }
        public Builder createdAt(Instant value) { this.createdAt = value; return this; }
        public Builder lastAttemptAt(Instant value) { this.lastAttemptAt = value; return this; }
        public Builder confirmedAt(Instant value) { this.confirmedAt = value; return this; }
        public Builder latestFailureAt(Instant value) { this.latestFailureAt = value; return this; }
        public Builder latestFailureCode(String value) { this.latestFailureCode = value; return this; }
        public Builder latestFailureMessage(String value) { this.latestFailureMessage = value; return this; }
        public Builder updatedAt(Instant value) { this.updatedAt = value; return this; }
        public PayoutTransfer build() { return new PayoutTransfer(this); }
    }
}

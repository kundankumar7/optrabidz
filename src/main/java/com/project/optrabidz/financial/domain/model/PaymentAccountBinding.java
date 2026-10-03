package com.project.optrabidz.financial.domain.model;

import org.springframework.util.Assert;

import java.time.Instant;

public class PaymentAccountBinding {
    private final Long paymentAccountBindingId;
    private final Long accountId;
    private final String providerCode;
    private final String commandIdempotencyKey;
    private final Long replacesBindingId;
    private final String externalRecipientReference;
    private PaymentAccountBindingStatus bindingStatus;
    private final String maskedAccountSuffix;
    private final String demoBankLabel;
    private final Instant createdAt;
    private Instant verifiedAt;
    private Instant deactivatedAt;
    private final Long lockVersion;

    private PaymentAccountBinding(Builder builder) {
        this.paymentAccountBindingId = builder.paymentAccountBindingId;
        this.accountId = builder.accountId;
        this.providerCode = builder.providerCode;
        this.commandIdempotencyKey = builder.commandIdempotencyKey;
        this.replacesBindingId = builder.replacesBindingId;
        this.externalRecipientReference = builder.externalRecipientReference;
        this.bindingStatus = builder.bindingStatus;
        this.maskedAccountSuffix = builder.maskedAccountSuffix;
        this.demoBankLabel = builder.demoBankLabel;
        this.createdAt = builder.createdAt;
        this.verifiedAt = builder.verifiedAt;
        this.deactivatedAt = builder.deactivatedAt;
        this.lockVersion = builder.lockVersion;
        validate();
    }

    public static PaymentAccountBinding createPending(Long accountId,
                                                       String providerCode,
                                                       String commandIdempotencyKey,
                                                       Long replacesBindingId,
                                                       String externalRecipientReference,
                                                       String maskedAccountSuffix,
                                                       String demoBankLabel,
                                                       Instant createdAt) {
        return builder()
                .accountId(accountId)
                .providerCode(providerCode)
                .commandIdempotencyKey(commandIdempotencyKey)
                .replacesBindingId(replacesBindingId)
                .externalRecipientReference(externalRecipientReference)
                .bindingStatus(PaymentAccountBindingStatus.PENDING_VERIFICATION)
                .maskedAccountSuffix(maskedAccountSuffix)
                .demoBankLabel(demoBankLabel)
                .createdAt(createdAt)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void verify(Instant now) {
        if (bindingStatus != PaymentAccountBindingStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Only pending binding can be verified");
        }
        requireNotBeforeCreation(now, "verifiedAt");
        bindingStatus = PaymentAccountBindingStatus.VERIFIED;
        verifiedAt = now;
    }

    public void deactivate(Instant now) {
        if (bindingStatus == PaymentAccountBindingStatus.DEACTIVATED) {
            throw new IllegalStateException("Binding is already deactivated");
        }
        requireNotBeforeCreation(now, "deactivatedAt");
        if (verifiedAt != null && now.isBefore(verifiedAt)) {
            throw new IllegalArgumentException("deactivatedAt must not be before verifiedAt");
        }
        bindingStatus = PaymentAccountBindingStatus.DEACTIVATED;
        deactivatedAt = now;
    }

    public boolean isReady() {
        return bindingStatus == PaymentAccountBindingStatus.VERIFIED;
    }

    private void validate() {
        Assert.notNull(accountId, "accountId must not be null");
        Assert.hasText(providerCode, "providerCode must not be blank");
        Assert.hasText(commandIdempotencyKey, "commandIdempotencyKey must not be blank");
        Assert.hasText(externalRecipientReference, "externalRecipientReference must not be blank");
        Assert.notNull(bindingStatus, "bindingStatus must not be null");
        Assert.hasText(maskedAccountSuffix, "maskedAccountSuffix must not be blank");
        Assert.hasText(demoBankLabel, "demoBankLabel must not be blank");
        Assert.notNull(createdAt, "createdAt must not be null");
        if (verifiedAt != null) {
            Assert.isTrue(!verifiedAt.isBefore(createdAt), "verifiedAt must not be before createdAt");
        }
        if (deactivatedAt != null) {
            Assert.isTrue(!deactivatedAt.isBefore(createdAt), "deactivatedAt must not be before createdAt");
        }
        Assert.isTrue(verifiedAt == null || deactivatedAt == null || !deactivatedAt.isBefore(verifiedAt),
                "deactivatedAt must not be before verifiedAt");
        switch (bindingStatus) {
            case PENDING_VERIFICATION -> Assert.isTrue(verifiedAt == null && deactivatedAt == null,
                    "Pending binding cannot have lifecycle timestamps");
            case VERIFIED -> Assert.isTrue(verifiedAt != null && deactivatedAt == null,
                    "Verified binding requires verifiedAt only");
            case DEACTIVATED -> Assert.notNull(deactivatedAt, "Deactivated binding requires deactivatedAt");
        }
    }

    private void requireNotBeforeCreation(Instant instant, String name) {
        Assert.notNull(instant, name + " must not be null");
        Assert.isTrue(!instant.isBefore(createdAt), name + " must not be before createdAt");
    }

    public Long getPaymentAccountBindingId() { return paymentAccountBindingId; }
    public Long getAccountId() { return accountId; }
    public String getProviderCode() { return providerCode; }
    public String getCommandIdempotencyKey() { return commandIdempotencyKey; }
    public Long getReplacesBindingId() { return replacesBindingId; }
    public String getExternalRecipientReference() { return externalRecipientReference; }
    public PaymentAccountBindingStatus getBindingStatus() { return bindingStatus; }
    public String getMaskedAccountSuffix() { return maskedAccountSuffix; }
    public String getDemoBankLabel() { return demoBankLabel; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
    public Long getLockVersion() { return lockVersion; }

    public static final class Builder {
        private Long paymentAccountBindingId;
        private Long accountId;
        private String providerCode;
        private String commandIdempotencyKey;
        private Long replacesBindingId;
        private String externalRecipientReference;
        private PaymentAccountBindingStatus bindingStatus;
        private String maskedAccountSuffix;
        private String demoBankLabel;
        private Instant createdAt;
        private Instant verifiedAt;
        private Instant deactivatedAt;
        private Long lockVersion;

        private Builder() {}

        public Builder paymentAccountBindingId(Long value) { this.paymentAccountBindingId = value; return this; }
        public Builder accountId(Long value) { this.accountId = value; return this; }
        public Builder providerCode(String value) { this.providerCode = value; return this; }
        public Builder commandIdempotencyKey(String value) { this.commandIdempotencyKey = value; return this; }
        public Builder replacesBindingId(Long value) { this.replacesBindingId = value; return this; }
        public Builder externalRecipientReference(String value) { this.externalRecipientReference = value; return this; }
        public Builder bindingStatus(PaymentAccountBindingStatus value) { this.bindingStatus = value; return this; }
        public Builder maskedAccountSuffix(String value) { this.maskedAccountSuffix = value; return this; }
        public Builder demoBankLabel(String value) { this.demoBankLabel = value; return this; }
        public Builder createdAt(Instant value) { this.createdAt = value; return this; }
        public Builder verifiedAt(Instant value) { this.verifiedAt = value; return this; }
        public Builder deactivatedAt(Instant value) { this.deactivatedAt = value; return this; }
        public Builder lockVersion(Long value) { this.lockVersion = value; return this; }
        public PaymentAccountBinding build() { return new PaymentAccountBinding(this); }
    }
}

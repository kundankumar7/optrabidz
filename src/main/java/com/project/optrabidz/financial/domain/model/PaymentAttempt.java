package com.project.optrabidz.financial.domain.model;

import org.springframework.util.Assert;

import java.time.Instant;

public class PaymentAttempt {
    private final Long paymentAttemptId;
    private final Long paymentIntentId;
    private final String providerCode;
    private final PaymentMethodType methodType;
    private String providerOrderId;
    private String providerPaymentId;
    private String providerReferenceId;
    private PaymentAttemptState attemptState;
    private final Instant createdAt;
    private Instant initiatedAt;
    private Instant confirmedAt;
    private Instant failedAt;
    private Instant expiredAt;
    private Instant cancelledAt;
    private String failureCode;
    private String failureMessage;
    private String providerPayload;

    private PaymentAttempt(Builder builder) {
        this.paymentAttemptId = builder.paymentAttemptId;
        this.paymentIntentId = builder.paymentIntentId;
        this.providerCode = builder.providerCode;
        this.methodType = builder.methodType;
        this.providerOrderId = builder.providerOrderId;
        this.providerPaymentId = builder.providerPaymentId;
        this.providerReferenceId = builder.providerReferenceId;
        this.attemptState = builder.attemptState;
        this.createdAt = builder.createdAt;
        this.initiatedAt = builder.initiatedAt;
        this.confirmedAt = builder.confirmedAt;
        this.failedAt = builder.failedAt;
        this.expiredAt = builder.expiredAt;
        this.cancelledAt = builder.cancelledAt;
        this.failureCode = builder.failureCode;
        this.failureMessage = builder.failureMessage;
        this.providerPayload = builder.providerPayload;
        validate();
    }

    public static PaymentAttempt create(Long paymentIntentId,
                                        String providerCode,
                                        PaymentMethodType methodType,
                                        Instant createdAt) {
        return builder()
                .paymentIntentId(paymentIntentId)
                .providerCode(providerCode)
                .methodType(methodType)
                .attemptState(PaymentAttemptState.CREATED)
                .createdAt(createdAt)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void markInitiated(String providerOrderId,
                              String providerReferenceId,
                              String providerPayload,
                              Instant now) {
        if (attemptState != PaymentAttemptState.CREATED) {
            throw new IllegalStateException("Only created payment attempt can be initiated");
        }
        this.providerOrderId = providerOrderId;
        this.providerReferenceId = providerReferenceId;
        this.providerPayload = providerPayload;
        this.attemptState = PaymentAttemptState.INITIATED;
        this.initiatedAt = now;
    }

    public void markConfirmed(String providerPaymentId, Instant now) {
        if (attemptState != PaymentAttemptState.CREATED && attemptState != PaymentAttemptState.INITIATED
                && attemptState != PaymentAttemptState.REQUIRES_ACTION) {
            throw new IllegalStateException("Only active payment attempt can be confirmed");
        }
        this.providerPaymentId = providerPaymentId;
        this.attemptState = PaymentAttemptState.CONFIRMED;
        this.confirmedAt = now;
    }

    public void markFailed(String failureCode, String failureMessage, Instant now) {
        if (attemptState == PaymentAttemptState.CONFIRMED || attemptState == PaymentAttemptState.CANCELLED
                || attemptState == PaymentAttemptState.EXPIRED) {
            throw new IllegalStateException("Final payment attempt cannot be failed");
        }
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.attemptState = PaymentAttemptState.FAILED;
        this.failedAt = now;
    }

    private void validate() {
        Assert.notNull(paymentIntentId, "paymentIntentId must not be null");
        Assert.hasText(providerCode, "providerCode must not be blank");
        Assert.notNull(methodType, "methodType must not be null");
        Assert.notNull(attemptState, "attemptState must not be null");
        Assert.notNull(createdAt, "createdAt must not be null");
    }

    public Long getPaymentAttemptId() {
        return paymentAttemptId;
    }

    public Long getPaymentIntentId() {
        return paymentIntentId;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public PaymentMethodType getMethodType() {
        return methodType;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public String getProviderReferenceId() {
        return providerReferenceId;
    }

    public PaymentAttemptState getAttemptState() {
        return attemptState;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getInitiatedAt() {
        return initiatedAt;
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

    public String getProviderPayload() {
        return providerPayload;
    }

    public static final class Builder {
        private Long paymentAttemptId;
        private Long paymentIntentId;
        private String providerCode;
        private PaymentMethodType methodType;
        private String providerOrderId;
        private String providerPaymentId;
        private String providerReferenceId;
        private PaymentAttemptState attemptState;
        private Instant createdAt;
        private Instant initiatedAt;
        private Instant confirmedAt;
        private Instant failedAt;
        private Instant expiredAt;
        private Instant cancelledAt;
        private String failureCode;
        private String failureMessage;
        private String providerPayload;

        private Builder() {
        }

        public Builder paymentAttemptId(Long paymentAttemptId) {
            this.paymentAttemptId = paymentAttemptId;
            return this;
        }

        public Builder paymentIntentId(Long paymentIntentId) {
            this.paymentIntentId = paymentIntentId;
            return this;
        }

        public Builder providerCode(String providerCode) {
            this.providerCode = providerCode;
            return this;
        }

        public Builder methodType(PaymentMethodType methodType) {
            this.methodType = methodType;
            return this;
        }

        public Builder providerOrderId(String providerOrderId) {
            this.providerOrderId = providerOrderId;
            return this;
        }

        public Builder providerPaymentId(String providerPaymentId) {
            this.providerPaymentId = providerPaymentId;
            return this;
        }

        public Builder providerReferenceId(String providerReferenceId) {
            this.providerReferenceId = providerReferenceId;
            return this;
        }

        public Builder attemptState(PaymentAttemptState attemptState) {
            this.attemptState = attemptState;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder initiatedAt(Instant initiatedAt) {
            this.initiatedAt = initiatedAt;
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

        public Builder providerPayload(String providerPayload) {
            this.providerPayload = providerPayload;
            return this;
        }

        public PaymentAttempt build() {
            return new PaymentAttempt(this);
        }
    }
}

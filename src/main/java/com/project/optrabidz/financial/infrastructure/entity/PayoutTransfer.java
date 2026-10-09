package com.project.optrabidz.financial.infrastructure.entity;

import com.project.optrabidz.financial.domain.model.PayoutTransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payout_transfer")
public class PayoutTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payout_transfer_id", nullable = false, updatable = false)
    private Long payoutTransferId;

    @Column(name = "payment_intent_id", nullable = false, updatable = false)
    private Long paymentIntentId;

    @Column(name = "payment_account_binding_id", nullable = false, updatable = false)
    private Long paymentAccountBindingId;

    @Column(name = "provider_code", nullable = false, updatable = false)
    private String providerCode;

    @Column(name = "provider_transfer_reference")
    private String providerTransferReference;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "amount", nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, updatable = false)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "transfer_status", nullable = false, columnDefinition = "payout_transfer_status_enum")
    private PayoutTransferStatus transferStatus;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "latest_failure_at")
    private Instant latestFailureAt;

    @Column(name = "latest_failure_code")
    private String latestFailureCode;

    @Column(name = "latest_failure_message")
    private String latestFailureMessage;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getPayoutTransferId() { return payoutTransferId; }
    public void setPayoutTransferId(Long value) { this.payoutTransferId = value; }
    public Long getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(Long value) { this.paymentIntentId = value; }
    public Long getPaymentAccountBindingId() { return paymentAccountBindingId; }
    public void setPaymentAccountBindingId(Long value) { this.paymentAccountBindingId = value; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String value) { this.providerCode = value; }
    public String getProviderTransferReference() { return providerTransferReference; }
    public void setProviderTransferReference(String value) { this.providerTransferReference = value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { this.idempotencyKey = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount = value; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String value) { this.currencyCode = value; }
    public PayoutTransferStatus getTransferStatus() { return transferStatus; }
    public void setTransferStatus(PayoutTransferStatus value) { this.transferStatus = value; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int value) { this.attemptCount = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant value) { this.lastAttemptAt = value; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant value) { this.confirmedAt = value; }
    public Instant getLatestFailureAt() { return latestFailureAt; }
    public void setLatestFailureAt(Instant value) { this.latestFailureAt = value; }
    public String getLatestFailureCode() { return latestFailureCode; }
    public void setLatestFailureCode(String value) { this.latestFailureCode = value; }
    public String getLatestFailureMessage() { return latestFailureMessage; }
    public void setLatestFailureMessage(String value) { this.latestFailureMessage = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}

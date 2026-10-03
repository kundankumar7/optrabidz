package com.project.optrabidz.financial.infrastructure.entity;

import com.project.optrabidz.financial.domain.model.PaymentAccountBindingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "payment_account_binding")
public class PaymentAccountBinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_account_binding_id", nullable = false, updatable = false)
    private Long paymentAccountBindingId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Column(name = "provider_code", nullable = false, updatable = false)
    private String providerCode;

    @Column(name = "command_idempotency_key", nullable = false, updatable = false)
    private String commandIdempotencyKey;

    @Column(name = "replaces_binding_id", updatable = false)
    private Long replacesBindingId;

    @Column(name = "external_recipient_reference", nullable = false, updatable = false)
    private String externalRecipientReference;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "binding_status", nullable = false, columnDefinition = "payment_account_binding_status_enum")
    private PaymentAccountBindingStatus bindingStatus;

    @Column(name = "masked_account_suffix", nullable = false, updatable = false)
    private String maskedAccountSuffix;

    @Column(name = "demo_bank_label", nullable = false, updatable = false)
    private String demoBankLabel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    public Long getPaymentAccountBindingId() { return paymentAccountBindingId; }
    public void setPaymentAccountBindingId(Long value) { this.paymentAccountBindingId = value; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long value) { this.accountId = value; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String value) { this.providerCode = value; }
    public String getCommandIdempotencyKey() { return commandIdempotencyKey; }
    public void setCommandIdempotencyKey(String value) { this.commandIdempotencyKey = value; }
    public Long getReplacesBindingId() { return replacesBindingId; }
    public void setReplacesBindingId(Long value) { this.replacesBindingId = value; }
    public String getExternalRecipientReference() { return externalRecipientReference; }
    public void setExternalRecipientReference(String value) { this.externalRecipientReference = value; }
    public PaymentAccountBindingStatus getBindingStatus() { return bindingStatus; }
    public void setBindingStatus(PaymentAccountBindingStatus value) { this.bindingStatus = value; }
    public String getMaskedAccountSuffix() { return maskedAccountSuffix; }
    public void setMaskedAccountSuffix(String value) { this.maskedAccountSuffix = value; }
    public String getDemoBankLabel() { return demoBankLabel; }
    public void setDemoBankLabel(String value) { this.demoBankLabel = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant value) { this.verifiedAt = value; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(Instant value) { this.deactivatedAt = value; }
    public Long getLockVersion() { return lockVersion; }
    public void setLockVersion(Long value) { this.lockVersion = value; }
}

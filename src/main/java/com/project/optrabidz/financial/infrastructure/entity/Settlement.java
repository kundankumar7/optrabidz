package com.project.optrabidz.financial.infrastructure.entity;

import com.project.optrabidz.financial.domain.model.SettlementState;
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
@Table(name = "settlement")
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id", nullable = false, updatable = false)
    private Long settlementId;

    @Column(name = "agreement_id", nullable = false)
    private Long agreementId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "settlement_state", nullable = false, columnDefinition = "settlement_state_enum")
    private SettlementState settlementState;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "confirmed_payment_intent_id")
    private Long confirmedPaymentIntentId;

    @Column(name = "psp_reference_id")
    private String pspReferenceId;

    public Long getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(Long settlementId) {
        this.settlementId = settlementId;
    }

    public Long getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(Long agreementId) {
        this.agreementId = agreementId;
    }

    public SettlementState getSettlementState() {
        return settlementState;
    }

    public void setSettlementState(SettlementState settlementState) {
        this.settlementState = settlementState;
    }

    public Long getStartupId() {
        return startupId;
    }

    public void setStartupId(Long startupId) {
        this.startupId = startupId;
    }

    public Long getInvestorId() {
        return investorId;
    }

    public void setInvestorId(Long investorId) {
        this.investorId = investorId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Long getConfirmedPaymentIntentId() {
        return confirmedPaymentIntentId;
    }

    public void setConfirmedPaymentIntentId(Long confirmedPaymentIntentId) {
        this.confirmedPaymentIntentId = confirmedPaymentIntentId;
    }

    public String getPspReferenceId() {
        return pspReferenceId;
    }

    public void setPspReferenceId(String pspReferenceId) {
        this.pspReferenceId = pspReferenceId;
    }
}

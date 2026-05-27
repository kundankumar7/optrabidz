package com.project.optrabidz.financial.infrastructure.entity;

import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
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
@Table(name = "repayment_installment")
public class RepaymentInstallment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repayment_installment_id", nullable = false, updatable = false)
    private Long repaymentInstallmentId;

    @Column(name = "repayment_id", nullable = false)
    private Long repaymentId;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "installment_status", nullable = false, columnDefinition = "repayment_installment_status_enum")
    private RepaymentInstallmentState installmentState;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "payment_started_at")
    private Instant paymentStartedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "overdue_at")
    private Instant overdueAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "confirmed_payment_intent_id")
    private Long confirmedPaymentIntentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getRepaymentInstallmentId() {
        return repaymentInstallmentId;
    }

    public void setRepaymentInstallmentId(Long repaymentInstallmentId) {
        this.repaymentInstallmentId = repaymentInstallmentId;
    }

    public Long getRepaymentId() {
        return repaymentId;
    }

    public void setRepaymentId(Long repaymentId) {
        this.repaymentId = repaymentId;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Integer installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public RepaymentInstallmentState getInstallmentState() {
        return installmentState;
    }

    public void setInstallmentState(RepaymentInstallmentState installmentState) {
        this.installmentState = installmentState;
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

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Instant getPaymentStartedAt() {
        return paymentStartedAt;
    }

    public void setPaymentStartedAt(Instant paymentStartedAt) {
        this.paymentStartedAt = paymentStartedAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public Instant getOverdueAt() {
        return overdueAt;
    }

    public void setOverdueAt(Instant overdueAt) {
        this.overdueAt = overdueAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

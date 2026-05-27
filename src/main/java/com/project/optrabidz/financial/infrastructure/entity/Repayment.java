package com.project.optrabidz.financial.infrastructure.entity;

import com.project.optrabidz.financial.domain.model.RepaymentState;
import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
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
@Table(name = "repayment")
public class Repayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repayment_id", nullable = false, updatable = false)
    private Long repaymentId;

    @Column(name = "agreement_id", nullable = false)
    private Long agreementId;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(name = "total_repayable_amount", nullable = false)
    private BigDecimal totalRepayableAmount;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(name = "total_installments", nullable = false)
    private Integer totalInstallments;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "repayment_plan_type", nullable = false, columnDefinition = "repayment_plan_type_enum")
    private RepaymentPlanType repaymentPlanType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "repayment_status", nullable = false, columnDefinition = "repayment_status_enum")
    private RepaymentState repaymentState;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "final_due_at", nullable = false)
    private Instant finalDueAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getRepaymentId() {
        return repaymentId;
    }

    public void setRepaymentId(Long repaymentId) {
        this.repaymentId = repaymentId;
    }

    public Long getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(Long agreementId) {
        this.agreementId = agreementId;
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

    public BigDecimal getTotalRepayableAmount() {
        return totalRepayableAmount;
    }

    public void setTotalRepayableAmount(BigDecimal totalRepayableAmount) {
        this.totalRepayableAmount = totalRepayableAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Integer getTotalInstallments() {
        return totalInstallments;
    }

    public void setTotalInstallments(Integer totalInstallments) {
        this.totalInstallments = totalInstallments;
    }

    public RepaymentPlanType getRepaymentPlanType() {
        return repaymentPlanType;
    }

    public void setRepaymentPlanType(RepaymentPlanType repaymentPlanType) {
        this.repaymentPlanType = repaymentPlanType;
    }

    public RepaymentState getRepaymentState() {
        return repaymentState;
    }

    public void setRepaymentState(RepaymentState repaymentState) {
        this.repaymentState = repaymentState;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinalDueAt() {
        return finalDueAt;
    }

    public void setFinalDueAt(Instant finalDueAt) {
        this.finalDueAt = finalDueAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

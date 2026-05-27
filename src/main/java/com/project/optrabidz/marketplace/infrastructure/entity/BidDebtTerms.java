package com.project.optrabidz.marketplace.infrastructure.entity;

import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bid_debt_terms")
public class BidDebtTerms {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_debt_terms_id", nullable = false, updatable = false)
    private Long bidDebtTermsId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bid_id", nullable = false, unique = true)
    private Bid bid;

    @Column(name = "proposed_amount", nullable = false)
    private BigDecimal proposedAmount;

    @Column(name = "proposed_interest_rate", nullable = false)
    private BigDecimal proposedInterestRate;

    @Column(name = "proposed_tenure_months", nullable = false)
    private Integer proposedTenureMonths;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "repayment_plan_type", nullable = false, columnDefinition = "repayment_plan_type_enum")
    private RepaymentPlanType repaymentPlanType;

    @Column(name = "one_time_repayment_due_after_months")
    private Integer oneTimeRepaymentDueAfterMonths;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getBidDebtTermsId() {
        return bidDebtTermsId;
    }

    public void setBidDebtTermsId(Long bidDebtTermsId) {
        this.bidDebtTermsId = bidDebtTermsId;
    }

    public Bid getBid() {
        return bid;
    }

    public void setBid(Bid bid) {
        this.bid = bid;
    }

    public BigDecimal getProposedAmount() {
        return proposedAmount;
    }

    public void setProposedAmount(BigDecimal proposedAmount) {
        this.proposedAmount = proposedAmount;
    }

    public BigDecimal getProposedInterestRate() {
        return proposedInterestRate;
    }

    public void setProposedInterestRate(BigDecimal proposedInterestRate) {
        this.proposedInterestRate = proposedInterestRate;
    }

    public Integer getProposedTenureMonths() {
        return proposedTenureMonths;
    }

    public void setProposedTenureMonths(Integer proposedTenureMonths) {
        this.proposedTenureMonths = proposedTenureMonths;
    }

    public RepaymentPlanType getRepaymentPlanType() {
        return repaymentPlanType;
    }

    public void setRepaymentPlanType(RepaymentPlanType repaymentPlanType) {
        this.repaymentPlanType = repaymentPlanType;
    }

    public Integer getOneTimeRepaymentDueAfterMonths() {
        return oneTimeRepaymentDueAfterMonths;
    }

    public void setOneTimeRepaymentDueAfterMonths(Integer oneTimeRepaymentDueAfterMonths) {
        this.oneTimeRepaymentDueAfterMonths = oneTimeRepaymentDueAfterMonths;
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

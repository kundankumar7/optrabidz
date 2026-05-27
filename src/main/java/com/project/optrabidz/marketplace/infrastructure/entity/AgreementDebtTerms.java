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
@Table(name = "agreement_debt_terms")
public class AgreementDebtTerms {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_debt_terms_id", nullable = false, updatable = false)
    private Long agreementDebtTermsId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id", nullable = false, unique = true)
    private Agreement agreement;

    @Column(name = "principal_amount", nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "repayment_plan_type", nullable = false, columnDefinition = "repayment_plan_type_enum")
    private RepaymentPlanType repaymentPlanType;

    @Column(name = "one_time_repayment_due_after_months")
    private Integer oneTimeRepaymentDueAfterMonths;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getAgreementDebtTermsId() {
        return agreementDebtTermsId;
    }

    public void setAgreementDebtTermsId(Long agreementDebtTermsId) {
        this.agreementDebtTermsId = agreementDebtTermsId;
    }

    public Agreement getAgreement() {
        return agreement;
    }

    public void setAgreement(Agreement agreement) {
        this.agreement = agreement;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
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
}

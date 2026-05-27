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
@Table(name = "listing_debt_terms")
public class ListingDebtTerms {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_debt_terms_id", nullable = false, updatable = false)
    private Long listingDebtTermsId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false, unique = true)
    private FundingListing listing;

    @Column(name = "requested_amount", nullable = false)
    private BigDecimal requestedAmount;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(name = "minimum_interest_rate")
    private BigDecimal minimumInterestRate;

    @Column(name = "maximum_interest_rate")
    private BigDecimal maximumInterestRate;

    @Column(name = "requested_tenure_months")
    private Integer requestedTenureMonths;

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

    public Long getListingDebtTermsId() {
        return listingDebtTermsId;
    }

    public void setListingDebtTermsId(Long listingDebtTermsId) {
        this.listingDebtTermsId = listingDebtTermsId;
    }

    public FundingListing getListing() {
        return listing;
    }

    public void setListing(FundingListing listing) {
        this.listing = listing;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getMinimumInterestRate() {
        return minimumInterestRate;
    }

    public void setMinimumInterestRate(BigDecimal minimumInterestRate) {
        this.minimumInterestRate = minimumInterestRate;
    }

    public BigDecimal getMaximumInterestRate() {
        return maximumInterestRate;
    }

    public void setMaximumInterestRate(BigDecimal maximumInterestRate) {
        this.maximumInterestRate = maximumInterestRate;
    }

    public Integer getRequestedTenureMonths() {
        return requestedTenureMonths;
    }

    public void setRequestedTenureMonths(Integer requestedTenureMonths) {
        this.requestedTenureMonths = requestedTenureMonths;
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

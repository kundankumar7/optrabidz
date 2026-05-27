package com.project.optrabidz.marketplace.infrastructure.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "agreement")
public class Agreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_id", nullable = false, updatable = false)
    private Long agreementId;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "bid_id", nullable = false, unique = true)
    private Long bidId;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToOne(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AgreementDebtTerms debtTerms;

    public Long getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(Long agreementId) {
        this.agreementId = agreementId;
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public Long getBidId() {
        return bidId;
    }

    public void setBidId(Long bidId) {
        this.bidId = bidId;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public AgreementDebtTerms getDebtTerms() {
        return debtTerms;
    }

    public void setDebtTerms(AgreementDebtTerms debtTerms) {
        this.debtTerms = debtTerms;
        if (debtTerms != null) {
            debtTerms.setAgreement(this);
        }
    }
}

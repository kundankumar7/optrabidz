package com.project.optrabidz.marketplace.infrastructure.entity;

import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "funding_listing")
public class FundingListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id", nullable = false, updatable = false)
    private Long listingId;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "listing_state", nullable = false, columnDefinition = "listing_state_enum")
    private ListingState listingState;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "funding_model", nullable = false, columnDefinition = "funding_model_enum")
    private FundingModel fundingModel;

    @Column(name = "funding_purpose_description", nullable = false)
    private String fundingPurposeDescription;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ListingDebtTerms debtTerms;

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public Long getStartupId() {
        return startupId;
    }

    public void setStartupId(Long startupId) {
        this.startupId = startupId;
    }

    public ListingState getListingState() {
        return listingState;
    }

    public void setListingState(ListingState listingState) {
        this.listingState = listingState;
    }

    public FundingModel getFundingModel() {
        return fundingModel;
    }

    public void setFundingModel(FundingModel fundingModel) {
        this.fundingModel = fundingModel;
    }

    public String getFundingPurposeDescription() {
        return fundingPurposeDescription;
    }

    public void setFundingPurposeDescription(String fundingPurposeDescription) {
        this.fundingPurposeDescription = fundingPurposeDescription;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public ListingDebtTerms getDebtTerms() {
        return debtTerms;
    }

    public void setDebtTerms(ListingDebtTerms debtTerms) {
        this.debtTerms = debtTerms;
        if (debtTerms != null) {
            debtTerms.setListing(this);
        }
    }
}

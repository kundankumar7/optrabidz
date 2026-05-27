package com.project.optrabidz.marketplace.domain.model;

import org.springframework.util.Assert;

import java.time.Instant;

public class FundingListing {
    private Long listingId;
    private Long startupId;
    private FundingModel fundingModel;
    private ListingState listingState;
    private String title;
    private String fundingPurposeDescription;
    private Instant createdAt;
    private Instant publishedAt;
    private Instant expiresAt;
    private Instant closedAt;
    private ListingDebtTerms debtTerms;

    protected FundingListing() {
    }

    private FundingListing(Builder builder) {
        this.listingId = builder.listingId;
        this.startupId = builder.startupId;
        this.fundingModel = builder.fundingModel;
        this.listingState = builder.listingState;
        this.title = builder.title;
        this.fundingPurposeDescription = builder.fundingPurposeDescription;
        this.createdAt = builder.createdAt;
        this.publishedAt = builder.publishedAt;
        this.expiresAt = builder.expiresAt;
        this.closedAt = builder.closedAt;
        this.debtTerms = builder.debtTerms;
        validateCore();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static FundingListing createDraft(Long startupId,
                                             FundingModel fundingModel,
                                             String title,
                                             String fundingPurposeDescription,
                                             ListingDebtTerms debtTerms,
                                             Instant createdAt) {
        return builder()
                .startupId(startupId)
                .fundingModel(fundingModel)
                .listingState(ListingState.DRAFT)
                .title(title)
                .fundingPurposeDescription(fundingPurposeDescription)
                .createdAt(createdAt)
                .debtTerms(debtTerms)
                .build();
    }

    public void updateDraft(String title,
                            String fundingPurposeDescription,
                            ListingDebtTerms debtTerms,
                            Instant updatedAt) {
        if (listingState != ListingState.DRAFT) {
            throw new IllegalStateException("Only DRAFT listings can be updated");
        }
        Assert.hasText(title, "title must not be blank");
        Assert.hasText(fundingPurposeDescription, "fundingPurposeDescription must not be blank");
        Assert.notNull(debtTerms, "debtTerms must not be null");
        this.title = title;
        this.fundingPurposeDescription = fundingPurposeDescription;
        this.debtTerms = debtTerms;
        if (this.debtTerms.getUpdatedAt() == null && updatedAt != null) {
            this.debtTerms.update(
                    debtTerms.getRequestedAmount(),
                    debtTerms.getCurrencyCode(),
                    debtTerms.getMinimumInterestRate(),
                    debtTerms.getMaximumInterestRate(),
                    debtTerms.getRequestedTenureMonths(),
                    debtTerms.getRepaymentPlanType(),
                    debtTerms.getOneTimeRepaymentDueAfterMonths(),
                    updatedAt
            );
        }
    }

    public void publish(Instant publishedAt, Instant expiresAt) {
        if (listingState != ListingState.DRAFT) {
            throw new IllegalStateException("Only DRAFT listings can be published");
        }
        Assert.notNull(publishedAt, "publishedAt must not be null");
        Assert.notNull(expiresAt, "expiresAt must not be null");
        Assert.isTrue(expiresAt.isAfter(publishedAt), "expiresAt must be after publishedAt");
        this.listingState = ListingState.OPEN;
        this.publishedAt = publishedAt;
        this.expiresAt = expiresAt;
    }

    public void close(Instant closedAt) {
        if (listingState != ListingState.OPEN) {
            throw new IllegalStateException("Only OPEN listings can be closed");
        }
        Assert.notNull(closedAt, "closedAt must not be null");
        this.listingState = ListingState.CLOSED;
        this.closedAt = closedAt;
    }

    public void markAgreementReached(Instant closedAt) {
        if (listingState != ListingState.OPEN) {
            throw new IllegalStateException("Only OPEN listings can reach agreement");
        }
        Assert.notNull(closedAt, "closedAt must not be null");
        this.listingState = ListingState.AGREEMENT_REACHED;
        this.closedAt = closedAt;
    }

    public boolean isOpen() {
        return listingState == ListingState.OPEN;
    }

    private void validateCore() {
        Assert.notNull(startupId, "startupId must not be null");
        Assert.notNull(fundingModel, "fundingModel must not be null");
        Assert.notNull(listingState, "listingState must not be null");
        Assert.hasText(title, "title must not be blank");
        Assert.hasText(fundingPurposeDescription, "fundingPurposeDescription must not be blank");
        Assert.notNull(createdAt, "createdAt must not be null");
        Assert.notNull(debtTerms, "debtTerms must not be null");
    }

    public Long getListingId() {
        return listingId;
    }

    public Long getStartupId() {
        return startupId;
    }

    public FundingModel getFundingModel() {
        return fundingModel;
    }

    public ListingState getListingState() {
        return listingState;
    }

    public String getTitle() {
        return title;
    }

    public String getFundingPurposeDescription() {
        return fundingPurposeDescription;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public ListingDebtTerms getDebtTerms() {
        return debtTerms;
    }

    public static final class Builder {
        private Long listingId;
        private Long startupId;
        private FundingModel fundingModel;
        private ListingState listingState;
        private String title;
        private String fundingPurposeDescription;
        private Instant createdAt;
        private Instant publishedAt;
        private Instant expiresAt;
        private Instant closedAt;
        private ListingDebtTerms debtTerms;

        private Builder() {
        }

        public Builder listingId(Long listingId) {
            this.listingId = listingId;
            return this;
        }

        public Builder startupId(Long startupId) {
            this.startupId = startupId;
            return this;
        }

        public Builder fundingModel(FundingModel fundingModel) {
            this.fundingModel = fundingModel;
            return this;
        }

        public Builder listingState(ListingState listingState) {
            this.listingState = listingState;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder fundingPurposeDescription(String fundingPurposeDescription) {
            this.fundingPurposeDescription = fundingPurposeDescription;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder publishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder closedAt(Instant closedAt) {
            this.closedAt = closedAt;
            return this;
        }

        public Builder debtTerms(ListingDebtTerms debtTerms) {
            this.debtTerms = debtTerms;
            return this;
        }

        public FundingListing build() {
            return new FundingListing(this);
        }
    }
}

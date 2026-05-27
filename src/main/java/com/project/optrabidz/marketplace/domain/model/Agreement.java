package com.project.optrabidz.marketplace.domain.model;

import org.springframework.util.Assert;

import java.time.Instant;

public class Agreement {
    private Long agreementId;
    private Long listingId;
    private Long bidId;
    private Long startupId;
    private Long investorId;
    private FundingModel fundingModel;
    private Instant createdAt;
    private AgreementDebtTerms debtTerms;

    private Agreement(Builder builder) {
        this.agreementId = builder.agreementId;
        this.listingId = builder.listingId;
        this.bidId = builder.bidId;
        this.startupId = builder.startupId;
        this.investorId = builder.investorId;
        this.fundingModel = builder.fundingModel;
        this.createdAt = builder.createdAt;
        this.debtTerms = builder.debtTerms;
        validateCore();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Agreement fromAcceptedBid(FundingListing listing,
                                            Bid bid,
                                            Long startupId,
                                            Instant createdAt) {
        Assert.notNull(listing, "listing must not be null");
        Assert.notNull(bid, "bid must not be null");
        Assert.isTrue(bid.getBidState() == BidState.ACCEPTED, "Agreement can be created only from accepted bid");
        return builder()
                .listingId(listing.getListingId())
                .bidId(bid.getBidId())
                .startupId(startupId)
                .investorId(bid.getInvestorId())
                .fundingModel(listing.getFundingModel())
                .createdAt(createdAt)
                .debtTerms(AgreementDebtTerms.fromBidTerms(bid.getDebtTerms(), createdAt))
                .build();
    }

    private void validateCore() {
        Assert.notNull(listingId, "listingId must not be null");
        Assert.notNull(bidId, "bidId must not be null");
        Assert.notNull(startupId, "startupId must not be null");
        Assert.notNull(investorId, "investorId must not be null");
        Assert.notNull(fundingModel, "fundingModel must not be null");
        Assert.notNull(createdAt, "createdAt must not be null");
        Assert.notNull(debtTerms, "debtTerms must not be null");
    }

    public Long getAgreementId() {
        return agreementId;
    }

    public Long getListingId() {
        return listingId;
    }

    public Long getBidId() {
        return bidId;
    }

    public Long getStartupId() {
        return startupId;
    }

    public Long getInvestorId() {
        return investorId;
    }

    public FundingModel getFundingModel() {
        return fundingModel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AgreementDebtTerms getDebtTerms() {
        return debtTerms;
    }

    public static final class Builder {
        private Long agreementId;
        private Long listingId;
        private Long bidId;
        private Long startupId;
        private Long investorId;
        private FundingModel fundingModel;
        private Instant createdAt;
        private AgreementDebtTerms debtTerms;

        private Builder() {
        }

        public Builder agreementId(Long agreementId) {
            this.agreementId = agreementId;
            return this;
        }

        public Builder listingId(Long listingId) {
            this.listingId = listingId;
            return this;
        }

        public Builder bidId(Long bidId) {
            this.bidId = bidId;
            return this;
        }

        public Builder startupId(Long startupId) {
            this.startupId = startupId;
            return this;
        }

        public Builder investorId(Long investorId) {
            this.investorId = investorId;
            return this;
        }

        public Builder fundingModel(FundingModel fundingModel) {
            this.fundingModel = fundingModel;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder debtTerms(AgreementDebtTerms debtTerms) {
            this.debtTerms = debtTerms;
            return this;
        }

        public Agreement build() {
            return new Agreement(this);
        }
    }
}

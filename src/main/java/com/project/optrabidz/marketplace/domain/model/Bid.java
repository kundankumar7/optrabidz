package com.project.optrabidz.marketplace.domain.model;

import org.springframework.util.Assert;

import java.time.Instant;

public class Bid {
    private Long bidId;
    private Long listingId;
    private Long investorId;
    private FundingModel fundingModel;
    private BidState bidState;
    private String proposalMessage;
    private Instant createdAt;
    private Instant withdrawnAt;
    private Instant rejectedAt;
    private Instant acceptedAt;
    private Instant fundedAt;
    private BidDebtTerms debtTerms;

    protected Bid() {
    }

    private Bid(Builder builder) {
        this.bidId = builder.bidId;
        this.listingId = builder.listingId;
        this.investorId = builder.investorId;
        this.fundingModel = builder.fundingModel;
        this.bidState = builder.bidState;
        this.proposalMessage = builder.proposalMessage;
        this.createdAt = builder.createdAt;
        this.withdrawnAt = builder.withdrawnAt;
        this.rejectedAt = builder.rejectedAt;
        this.acceptedAt = builder.acceptedAt;
        this.fundedAt = builder.fundedAt;
        this.debtTerms = builder.debtTerms;
        validateCore();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Bid submit(Long listingId,
                             Long investorId,
                             FundingModel fundingModel,
                             BidDebtTerms debtTerms,
                             String proposalMessage,
                             Instant createdAt) {
        return builder()
                .listingId(listingId)
                .investorId(investorId)
                .fundingModel(fundingModel)
                .bidState(BidState.SUBMITTED)
                .debtTerms(debtTerms)
                .proposalMessage(proposalMessage)
                .createdAt(createdAt)
                .build();
    }

    public void withdraw(Instant withdrawnAt) {
        if (bidState != BidState.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED bids can be withdrawn");
        }
        this.bidState = BidState.WITHDRAWN;
        this.withdrawnAt = withdrawnAt;
    }

    public void reject(Instant rejectedAt) {
        if (bidState != BidState.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED bids can be rejected");
        }
        this.bidState = BidState.REJECTED;
        this.rejectedAt = rejectedAt;
    }

    public void accept(Instant acceptedAt) {
        if (bidState != BidState.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED bids can be accepted");
        }
        this.bidState = BidState.ACCEPTED;
        this.acceptedAt = acceptedAt;
    }

    private void validateCore() {
        Assert.notNull(listingId, "listingId must not be null");
        Assert.notNull(investorId, "investorId must not be null");
        Assert.notNull(fundingModel, "fundingModel must not be null");
        Assert.notNull(bidState, "bidState must not be null");
        Assert.notNull(createdAt, "createdAt must not be null");
        Assert.notNull(debtTerms, "debtTerms must not be null");
    }

    public Long getBidId() {
        return bidId;
    }

    public Long getListingId() {
        return listingId;
    }

    public Long getInvestorId() {
        return investorId;
    }

    public FundingModel getFundingModel() {
        return fundingModel;
    }

    public BidState getBidState() {
        return bidState;
    }

    public String getProposalMessage() {
        return proposalMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getFundedAt() {
        return fundedAt;
    }

    public BidDebtTerms getDebtTerms() {
        return debtTerms;
    }

    public static final class Builder {
        private Long bidId;
        private Long listingId;
        private Long investorId;
        private FundingModel fundingModel;
        private BidState bidState;
        private String proposalMessage;
        private Instant createdAt;
        private Instant withdrawnAt;
        private Instant rejectedAt;
        private Instant acceptedAt;
        private Instant fundedAt;
        private BidDebtTerms debtTerms;

        private Builder() {
        }

        public Builder bidId(Long bidId) {
            this.bidId = bidId;
            return this;
        }

        public Builder listingId(Long listingId) {
            this.listingId = listingId;
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

        public Builder bidState(BidState bidState) {
            this.bidState = bidState;
            return this;
        }

        public Builder proposalMessage(String proposalMessage) {
            this.proposalMessage = proposalMessage;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder withdrawnAt(Instant withdrawnAt) {
            this.withdrawnAt = withdrawnAt;
            return this;
        }

        public Builder rejectedAt(Instant rejectedAt) {
            this.rejectedAt = rejectedAt;
            return this;
        }

        public Builder acceptedAt(Instant acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public Builder fundedAt(Instant fundedAt) {
            this.fundedAt = fundedAt;
            return this;
        }

        public Builder debtTerms(BidDebtTerms debtTerms) {
            this.debtTerms = debtTerms;
            return this;
        }

        public Bid build() {
            return new Bid(this);
        }
    }
}

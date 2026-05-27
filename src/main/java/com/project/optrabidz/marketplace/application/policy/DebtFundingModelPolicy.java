package com.project.optrabidz.marketplace.application.policy;

import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class DebtFundingModelPolicy implements FundingModelPolicy {
    @Override
    public FundingModel supports() {
        return FundingModel.DEBT;
    }

    @Override
    public void validateListing(FundingListing listing) {
        Assert.notNull(listing, "listing must not be null");
        Assert.isTrue(listing.getFundingModel() == FundingModel.DEBT, "Only DEBT listing is supported currently");
        Assert.notNull(listing.getDebtTerms(), "debtTerms are required for DEBT listing");
    }

    @Override
    public void validateBid(FundingListing listing, Bid bid) {
        validateListing(listing);
        Assert.notNull(bid, "bid must not be null");
        Assert.isTrue(bid.getFundingModel() == FundingModel.DEBT, "Only DEBT bid is supported currently");
        Assert.isTrue(bid.getFundingModel() == listing.getFundingModel(), "Bid funding model must match listing");
        Assert.notNull(bid.getDebtTerms(), "debtTerms are required for DEBT bid");
    }
}

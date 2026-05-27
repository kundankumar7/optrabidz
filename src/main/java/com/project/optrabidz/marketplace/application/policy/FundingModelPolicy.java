package com.project.optrabidz.marketplace.application.policy;

import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;

public interface FundingModelPolicy {
    FundingModel supports();

    void validateListing(FundingListing listing);

    void validateBid(FundingListing listing, Bid bid);
}

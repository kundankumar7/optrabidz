package com.project.optrabidz.marketplace.application.specification;

import com.project.optrabidz.marketplace.application.exception.InvalidListingStateException;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import org.springframework.stereotype.Component;

@Component
public class ListingCanBePublishedSpec {
    public void assertSatisfiedBy(FundingListing listing) {
        if (listing.getListingState() != ListingState.DRAFT) {
            throw new InvalidListingStateException("Only DRAFT listings can be published");
        }
    }
}

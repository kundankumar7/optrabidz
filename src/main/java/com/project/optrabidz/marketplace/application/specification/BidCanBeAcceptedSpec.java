package com.project.optrabidz.marketplace.application.specification;

import com.project.optrabidz.marketplace.application.exception.BidAlreadyAcceptedException;
import com.project.optrabidz.marketplace.application.exception.InvalidBidStateException;
import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.BidState;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import org.springframework.stereotype.Component;

@Component
public class BidCanBeAcceptedSpec {
    public void assertSatisfiedBy(FundingListing listing, Bid bid, boolean listingAlreadyHasAcceptedBid) {
        if (listing.getListingState() != ListingState.OPEN) {
            throw new BidAlreadyAcceptedException("Listing is not open for bid acceptance");
        }
        if (bid.getBidState() != BidState.SUBMITTED) {
            throw new InvalidBidStateException("Only SUBMITTED bids can be accepted");
        }
        if (listingAlreadyHasAcceptedBid) {
            throw new BidAlreadyAcceptedException("Listing already has an accepted bid");
        }
    }
}

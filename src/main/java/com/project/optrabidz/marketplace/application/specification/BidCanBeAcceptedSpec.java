package com.project.optrabidz.marketplace.application.specification;

import com.project.optrabidz.marketplace.application.exception.BidAcceptanceConflictException;
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
            throw new BidAcceptanceConflictException(
                    "Listing " + listing.getListingId()
                            + " is in state " + listing.getListingState()
                            + " and cannot accept bid " + bid.getBidId()
            );
        }
        if (bid.getBidState() != BidState.SUBMITTED) {
            throw new InvalidBidStateException("Only SUBMITTED bids can be accepted");
        }
        if (listingAlreadyHasAcceptedBid) {
            throw new BidAcceptanceConflictException(
                    "Listing " + listing.getListingId()
                            + " already has an accepted bid; candidate bid="
                            + bid.getBidId()
            );
        }
    }
}

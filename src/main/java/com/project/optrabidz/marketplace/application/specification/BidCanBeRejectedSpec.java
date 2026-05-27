package com.project.optrabidz.marketplace.application.specification;

import com.project.optrabidz.marketplace.application.exception.InvalidBidStateException;
import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.BidState;
import org.springframework.stereotype.Component;

@Component
public class BidCanBeRejectedSpec {
    public void assertSatisfiedBy(Bid bid) {
        if (bid.getBidState() != BidState.SUBMITTED) {
            throw new InvalidBidStateException("Only SUBMITTED bids can be rejected");
        }
    }
}

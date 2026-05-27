package com.project.optrabidz.marketplace.application.factory;

import com.project.optrabidz.marketplace.domain.model.Agreement;
import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AgreementFactory {
    public Agreement createFromAcceptedBid(FundingListing listing, Bid bid, Instant now) {
        return Agreement.fromAcceptedBid(listing, bid, listing.getStartupId(), now);
    }
}

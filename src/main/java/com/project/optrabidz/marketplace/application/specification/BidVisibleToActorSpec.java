package com.project.optrabidz.marketplace.application.specification;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.application.exception.MarketplaceAccessException;
import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import org.springframework.stereotype.Component;

@Component
public class BidVisibleToActorSpec {
    public void assertSatisfiedBy(RoleType roleType,
                                  Bid bid,
                                  Long requesterInvestorId,
                                  Long requesterStartupId,
                                  FundingListing listing) {
        if (roleType == RoleType.INVESTOR
                && requesterInvestorId != null
                && requesterInvestorId.equals(bid.getInvestorId())) {
            return;
        }
        if (roleType == RoleType.STARTUP
                && requesterStartupId != null
                && requesterStartupId.equals(listing.getStartupId())) {
            return;
        }
        if (roleType == RoleType.ADMIN) {
            return;
        }
        throw new MarketplaceAccessException("You are not authorized to view this bid");
    }
}

package com.project.optrabidz.marketplace.application.specification;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.application.exception.MarketplaceAccessException;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.participation.domain.model.Startup;
import org.springframework.stereotype.Component;

@Component
public class ListingVisibleToActorSpec {
    public void assertSatisfiedBy(FundingListing listing,
                                  Long requesterAccountId,
                                  RoleType requesterRole,
                                  Startup listingStartup) {
        if (listing.isOpen()) {
            return;
        }
        if (requesterAccountId == null || requesterRole == null) {
            throw new MarketplaceAccessException("Authentication is required to view this listing");
        }
        if (requesterRole == RoleType.ADMIN) {
            return;
        }
        if (requesterRole == RoleType.STARTUP && listingStartup.getAccountId().equals(requesterAccountId)) {
            return;
        }
        throw new MarketplaceAccessException("You are not authorized to view this listing");
    }
}

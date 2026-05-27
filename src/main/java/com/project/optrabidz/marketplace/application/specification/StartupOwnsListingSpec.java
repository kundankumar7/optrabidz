package com.project.optrabidz.marketplace.application.specification;

import com.project.optrabidz.marketplace.application.exception.MarketplaceAccessException;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.participation.domain.model.Startup;
import org.springframework.stereotype.Component;

@Component
public class StartupOwnsListingSpec {
    public void assertSatisfiedBy(Startup startup, FundingListing listing) {
        if (!startup.getStartupId().equals(listing.getStartupId())) {
            throw new MarketplaceAccessException("Startup can access only owned listings");
        }
    }
}

package com.project.optrabidz.marketplace.application.specification;

import com.project.optrabidz.marketplace.application.exception.MarketplaceAccessException;
import com.project.optrabidz.participation.domain.model.Startup;
import org.springframework.stereotype.Component;

@Component
public class InvestorCannotBidOnOwnListingSpec {
    public void assertSatisfiedBy(Long investorAccountId, Startup listingStartup) {
        if (listingStartup.getAccountId().equals(investorAccountId)) {
            throw new MarketplaceAccessException("Investor cannot bid on own startup listing");
        }
    }
}

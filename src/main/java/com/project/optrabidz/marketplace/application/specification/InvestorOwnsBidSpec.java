package com.project.optrabidz.marketplace.application.specification;

import com.project.optrabidz.marketplace.application.exception.MarketplaceAccessException;
import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.participation.domain.model.Investor;
import org.springframework.stereotype.Component;

@Component
public class InvestorOwnsBidSpec {
    public void assertSatisfiedBy(Investor investor, Bid bid) {
        if (!investor.getInvestorId().equals(bid.getInvestorId())) {
            throw new MarketplaceAccessException("Investor can withdraw only own bid");
        }
    }
}

package com.project.optrabidz.marketplace.infrastructure.adapter;

import com.project.optrabidz.governance.application.port.MarketplaceLifecycleGovernancePort;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MarketplaceLifecycleGovernanceAdapter implements MarketplaceLifecycleGovernancePort {
    private final FundingListingRepository fundingListingRepository;

    public MarketplaceLifecycleGovernanceAdapter(FundingListingRepository fundingListingRepository) {
        this.fundingListingRepository = fundingListingRepository;
    }

    @Override
    public int expireOpenListings(Instant now, int batchSize) {
        return fundingListingRepository.expireOpenListings(now, batchSize);
    }
}

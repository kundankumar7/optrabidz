package com.project.optrabidz.marketplace.domain.repository;

import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingSortMode;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface FundingListingRepository {
    FundingListing save(FundingListing listing);

    Optional<FundingListing> findById(Long listingId);

    Page<FundingListing> findByStartupId(Long startupId,
                                          ListingState state,
                                          FundingModel fundingModel,
                                          Pageable pageable);

    Page<FundingListing> findOpenListings(FundingModel fundingModel,
                                          java.math.BigDecimal minAmount,
                                          java.math.BigDecimal maxAmount,
                                          String currencyCode,
                                          ListingSortMode sortMode,
                                          Pageable pageable);

    int expireOpenListings(Instant now, int batchSize);

    int markAgreementReachedIfOpen(Long listingId, Instant closedAt);
}

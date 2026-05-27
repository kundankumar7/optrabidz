package com.project.optrabidz.marketplace.infrastructure.repository;

import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingSortMode;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import com.project.optrabidz.marketplace.infrastructure.mapper.MarketplacePersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class FundingListingRepositoryAdapter implements FundingListingRepository {
    private final JpaFundingListingRepository jpaFundingListingRepository;
    private final MarketplacePersistenceMapper mapper;

    public FundingListingRepositoryAdapter(JpaFundingListingRepository jpaFundingListingRepository,
                                           MarketplacePersistenceMapper mapper) {
        this.jpaFundingListingRepository = jpaFundingListingRepository;
        this.mapper = mapper;
    }

    @Override
    public FundingListing save(FundingListing listing) {
        return mapper.toDomain(jpaFundingListingRepository.save(mapper.toEntity(listing)));
    }

    @Override
    public Optional<FundingListing> findById(Long listingId) {
        return jpaFundingListingRepository.findById(listingId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<FundingListing> findByStartupId(Long startupId,
                                                ListingState state,
                                                FundingModel fundingModel,
                                                Pageable pageable) {
        return jpaFundingListingRepository.findOwnedListings(
                        startupId,
                        state == null ? null : state.name(),
                        fundingModel == null ? null : fundingModel.name(),
                        unsorted(pageable)
                )
                .map(mapper::toDomain);
    }

    @Override
    public Page<FundingListing> findOpenListings(FundingModel fundingModel,
                                                 BigDecimal minAmount,
                                                 BigDecimal maxAmount,
                                                 String currencyCode,
                                                 ListingSortMode sortMode,
                                                 Pageable pageable) {
        String normalizedCurrency = currencyCode == null || currencyCode.isBlank()
                ? null
                : currencyCode.trim().toUpperCase();
        return jpaFundingListingRepository.findOpenListings(
                        fundingModel == null ? null : fundingModel.name(),
                        minAmount,
                        maxAmount,
                        normalizedCurrency,
                        sortMode == null ? ListingSortMode.NEWEST.name() : sortMode.name(),
                        unsorted(pageable)
                )
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public int expireOpenListings(Instant now, int batchSize) {
        if (batchSize <= 0) {
            return 0;
        }
        List<Long> ids = jpaFundingListingRepository.findExpiredOpenListingIds(now, batchSize);
        if (ids.isEmpty()) {
            return 0;
        }
        return jpaFundingListingRepository.closeOpenListings(ids, now);
    }

    @Override
    @Transactional
    public int markAgreementReachedIfOpen(Long listingId, Instant closedAt) {
        return jpaFundingListingRepository.markAgreementReachedIfOpen(listingId, closedAt);
    }

    private Pageable unsorted(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}

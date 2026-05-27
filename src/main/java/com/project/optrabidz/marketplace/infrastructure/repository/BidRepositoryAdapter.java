package com.project.optrabidz.marketplace.infrastructure.repository;

import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.BidState;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.repository.BidRepository;
import com.project.optrabidz.marketplace.infrastructure.mapper.MarketplacePersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class BidRepositoryAdapter implements BidRepository {
    private final JpaBidRepository jpaBidRepository;
    private final JpaFundingListingRepository jpaFundingListingRepository;
    private final MarketplacePersistenceMapper mapper;

    public BidRepositoryAdapter(JpaBidRepository jpaBidRepository,
                                JpaFundingListingRepository jpaFundingListingRepository,
                                MarketplacePersistenceMapper mapper) {
        this.jpaBidRepository = jpaBidRepository;
        this.jpaFundingListingRepository = jpaFundingListingRepository;
        this.mapper = mapper;
    }

    @Override
    public Bid save(Bid bid) {
        return mapper.toDomain(jpaBidRepository.save(mapper.toEntity(bid)), bid.getFundingModel());
    }

    @Override
    public Bid saveAndFlush(Bid bid) {
        return mapper.toDomain(jpaBidRepository.saveAndFlush(mapper.toEntity(bid)), bid.getFundingModel());
    }

    @Override
    public Optional<Bid> findById(Long bidId) {
        return jpaBidRepository.findById(bidId).map(this::toDomain);
    }

    @Override
    public Optional<Bid> findAcceptedByListingId(Long listingId) {
        return jpaBidRepository.findAcceptedByListingId(listingId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Bid> findLatestByInvestorIdAndListingId(Long investorId, Long listingId) {
        return jpaBidRepository.findFirstByInvestorIdAndListingIdOrderByCreatedAtDesc(investorId, listingId)
                .map(this::toDomain);
    }

    @Override
    public boolean existsActiveByInvestorIdAndListingId(Long investorId, Long listingId) {
        return jpaBidRepository.existsActiveByInvestorIdAndListingId(investorId, listingId);
    }

    @Override
    public boolean existsAcceptedByListingId(Long listingId) {
        return jpaBidRepository.existsAcceptedByListingId(listingId);
    }

    @Override
    public int rejectOtherSubmittedBids(Long listingId, Long acceptedBidId, Instant rejectedAt) {
        return jpaBidRepository.rejectOtherSubmittedBids(listingId, acceptedBidId, rejectedAt);
    }

    @Override
    public Page<Bid> findByListingId(Long listingId, BidState state, Pageable pageable) {
        return toDomainPage(jpaBidRepository.findByListingIdAndOptionalState(
                        listingId,
                        state == null ? null : state.name(),
                        unsorted(pageable)
                ));
    }

    @Override
    public Page<Bid> findByInvestorId(Long investorId, BidState state, Pageable pageable) {
        return toDomainPage(jpaBidRepository.findByInvestorIdAndOptionalState(
                        investorId,
                        state == null ? null : state.name(),
                        unsorted(pageable)
                ));
    }

    private Bid toDomain(com.project.optrabidz.marketplace.infrastructure.entity.Bid entity) {
        FundingModel fundingModel = jpaFundingListingRepository.findById(entity.getListingId())
                .map(com.project.optrabidz.marketplace.infrastructure.entity.FundingListing::getFundingModel)
                .orElse(FundingModel.DEBT);
        return mapper.toDomain(entity, fundingModel);
    }

    private Page<Bid> toDomainPage(Page<com.project.optrabidz.marketplace.infrastructure.entity.Bid> bids) {
        Map<Long, FundingModel> fundingModels = fundingModelsByListingId(bids);
        return bids.map(entity -> mapper.toDomain(
                entity,
                fundingModels.getOrDefault(entity.getListingId(), FundingModel.DEBT)
        ));
    }

    private Map<Long, FundingModel> fundingModelsByListingId(
            Page<com.project.optrabidz.marketplace.infrastructure.entity.Bid> bids) {
        var listingIds = bids.getContent()
                .stream()
                .map(com.project.optrabidz.marketplace.infrastructure.entity.Bid::getListingId)
                .collect(Collectors.toSet());
        if (listingIds.isEmpty()) {
            return Map.of();
        }
        return jpaFundingListingRepository.findFundingModelsByListingIdIn(listingIds)
                .stream()
                .collect(Collectors.toMap(
                        JpaFundingListingRepository.ListingFundingModelView::getListingId,
                        JpaFundingListingRepository.ListingFundingModelView::getFundingModel,
                        (first, second) -> first
                ));
    }

    private Pageable unsorted(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}

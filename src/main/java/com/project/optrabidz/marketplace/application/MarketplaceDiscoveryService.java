package com.project.optrabidz.marketplace.application;

import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.application.dto.response.ListingResponse;
import com.project.optrabidz.marketplace.application.dto.response.RecommendedListingResponse;
import com.project.optrabidz.marketplace.application.recommendation.MarketplaceRecommendationPipeline;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationCandidate;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationRequest;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingSortMode;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import com.project.optrabidz.participation.application.exception.InvalidRoleException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MarketplaceDiscoveryService {
    private static final int RECOMMENDATION_CANDIDATE_LIMIT = 200;

    private final FundingListingRepository listingRepository;
    private final ListingService listingService;
    private final MarketplaceRecommendationPipeline recommendationPipeline;

    public MarketplaceDiscoveryService(FundingListingRepository listingRepository,
                                       ListingService listingService,
                                       MarketplaceRecommendationPipeline recommendationPipeline) {
        this.listingRepository = listingRepository;
        this.listingService = listingService;
        this.recommendationPipeline = recommendationPipeline;
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> browseOpenListings(FundingModel fundingModel,
                                                            BigDecimal minAmount,
                                                            BigDecimal maxAmount,
                                                            String currencyCode,
                                                            String sort,
                                                            int page,
                                                            int size) {
        ListingSortMode sortMode = ListingSortMode.from(sort);
        Pageable pageable = PageRequest.of(toPageIndex(page), safeSize(size));
        Page<ListingResponse> listings = listingRepository.findOpenListings(
                        fundingModel,
                        minAmount,
                        maxAmount,
                        currencyCode,
                        sortMode,
                        pageable
                )
                .map(listingService::toListingResponse);
        return new PageResponse<>(
                listings.getContent(),
                Math.max(page, 1),
                safeSize(size),
                listings.getTotalElements(),
                listings.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<RecommendedListingResponse> getRecommendedListings(Long accountId,
                                                                           RoleType roleType,
                                                                           FundingModel fundingModel,
                                                                           BigDecimal minAmount,
                                                                           BigDecimal maxAmount,
                                                                           String currencyCode,
                                                                           int page,
                                                                           int size) {
        if (roleType != RoleType.INVESTOR) {
            throw new InvalidRoleException("Role is not allowed to perform this operation");
        }

        RecommendationRequest request = new RecommendationRequest(
                accountId,
                fundingModel,
                minAmount,
                maxAmount,
                currencyCode,
                page,
                size,
                RECOMMENDATION_CANDIDATE_LIMIT
        );

        List<RecommendationCandidate> ranked = recommendationPipeline.recommend(request);

        int safePage = request.page();
        int safeSize = request.size();
        int from = Math.min((safePage - 1) * safeSize, ranked.size());
        int to = Math.min(from + safeSize, ranked.size());
        List<RecommendedListingResponse> items = ranked.subList(from, to)
                .stream()
                .map(this::toRecommendedListingResponse)
                .toList();
        int totalPages = ranked.isEmpty() ? 0 : (int) Math.ceil((double) ranked.size() / safeSize);

        return new PageResponse<>(items, safePage, safeSize, ranked.size(), totalPages);
    }

    private RecommendedListingResponse toRecommendedListingResponse(RecommendationCandidate candidate) {
        return new RecommendedListingResponse(
                listingService.toListingResponse(candidate.listing()),
                candidate.toRecommendationInfo()
        );
    }

    private int toPageIndex(int page) {
        return Math.max(page, 1) - 1;
    }

    private int safeSize(int size) {
        return Math.max(size, 1);
    }
}

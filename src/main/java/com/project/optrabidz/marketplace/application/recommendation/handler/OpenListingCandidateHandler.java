package com.project.optrabidz.marketplace.application.recommendation.handler;

import com.project.optrabidz.marketplace.application.recommendation.RecommendationCandidate;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationContext;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationHandler;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationRequest;
import com.project.optrabidz.marketplace.domain.model.ListingSortMode;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class OpenListingCandidateHandler implements RecommendationHandler {
    private final FundingListingRepository listingRepository;

    public OpenListingCandidateHandler(FundingListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Override
    public void handle(RecommendationContext context) {
        RecommendationRequest request = context.request();
        context.candidates(listingRepository.findOpenListings(
                        request.fundingModel(),
                        request.minAmount(),
                        request.maxAmount(),
                        request.currencyCode(),
                        ListingSortMode.NEWEST,
                        PageRequest.of(0, request.candidateLimit())
                )
                .stream()
                .map(RecommendationCandidate::new)
                .toList());
    }
}

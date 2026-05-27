package com.project.optrabidz.marketplace.application.recommendation;

import com.project.optrabidz.classification.application.dto.response.ClassificationEntryResponse;
import com.project.optrabidz.classification.application.dto.response.InvestorPreferenceResponse;
import com.project.optrabidz.classification.application.dto.response.StartupClassificationResponse;
import com.project.optrabidz.classification.application.port.in.InvestorPreferenceQueryPort;
import com.project.optrabidz.classification.application.port.in.StartupClassificationQueryPort;
import com.project.optrabidz.marketplace.application.recommendation.handler.ClassificationMatchScoringHandler;
import com.project.optrabidz.marketplace.application.recommendation.handler.DefaultRecommendationReasonHandler;
import com.project.optrabidz.marketplace.application.recommendation.handler.FundingModelCompatibilityScoringHandler;
import com.project.optrabidz.marketplace.application.recommendation.handler.InvestorPreferenceLoadHandler;
import com.project.optrabidz.marketplace.application.recommendation.handler.OpenListingCandidateHandler;
import com.project.optrabidz.marketplace.application.recommendation.handler.RecencyBoostScoringHandler;
import com.project.optrabidz.marketplace.application.recommendation.handler.RecommendationRankingHandler;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingDebtTerms;
import com.project.optrabidz.marketplace.domain.model.ListingSortMode;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceRecommendationPipelineTest {
    private static final Long ACCOUNT_ID = 101L;
    private static final Instant NOW = Instant.now();

    @Mock
    private FundingListingRepository listingRepository;

    @Mock
    private InvestorPreferenceQueryPort investorPreferenceQueryPort;

    @Mock
    private StartupClassificationQueryPort startupClassificationQueryPort;

    private MarketplaceRecommendationPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new MarketplaceRecommendationPipeline(List.of(
                new RecommendationRankingHandler(),
                new RecencyBoostScoringHandler(),
                new OpenListingCandidateHandler(listingRepository),
                new DefaultRecommendationReasonHandler(),
                new FundingModelCompatibilityScoringHandler(),
                new ClassificationMatchScoringHandler(startupClassificationQueryPort),
                new InvestorPreferenceLoadHandler(investorPreferenceQueryPort)
        ));
    }

    @Test
    void recommendationPipelineScoresAndRanksMatchingListingsFirst() {
        FundingListing unmatchedListing = listing(2L, FundingModel.DEBT, NOW.minusSeconds(3_600));
        FundingListing matchedListing = listing(1L, FundingModel.DEBT, NOW.minusSeconds(3_600));
        RecommendationRequest request = new RecommendationRequest(
                ACCOUNT_ID,
                FundingModel.DEBT,
                new BigDecimal("100000.00"),
                new BigDecimal("900000.00"),
                "INR",
                1,
                20,
                200
        );

        when(investorPreferenceQueryPort.getMyPreferences(ACCOUNT_ID))
                .thenReturn(new InvestorPreferenceResponse(10L, List.of(
                        new ClassificationEntryResponse("INDUSTRY", "SAAS"),
                        new ClassificationEntryResponse("STAGE", "SEED")
                )));
        when(listingRepository.findOpenListings(
                eq(FundingModel.DEBT),
                eq(new BigDecimal("100000.00")),
                eq(new BigDecimal("900000.00")),
                eq("INR"),
                eq(ListingSortMode.NEWEST),
                eq(PageRequest.of(0, 200))
        )).thenReturn(new PageImpl<>(List.of(unmatchedListing, matchedListing)));
        when(startupClassificationQueryPort.getStartupClassifications(1L))
                .thenReturn(new StartupClassificationResponse(1L, List.of(
                        new ClassificationEntryResponse("INDUSTRY", "SAAS"),
                        new ClassificationEntryResponse("STAGE", "SEED")
                )));
        when(startupClassificationQueryPort.getStartupClassifications(2L))
                .thenReturn(new StartupClassificationResponse(2L, List.of(
                        new ClassificationEntryResponse("INDUSTRY", "MANUFACTURING")
                )));

        List<RecommendationCandidate> recommendations = pipeline.recommend(request);

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations.get(0).listing().getStartupId()).isEqualTo(1L);
        assertThat(recommendations.get(0).score()).isEqualTo(80);
        assertThat(recommendations.get(0).toRecommendationInfo().reasons())
                .containsExactly(
                        "Matches currently supported funding model",
                        "Matches investor classification preferences",
                        "Recently published"
                );
        assertThat(recommendations.get(1).listing().getStartupId()).isEqualTo(2L);
        assertThat(recommendations.get(1).score()).isEqualTo(30);
        verify(investorPreferenceQueryPort).getMyPreferences(ACCOUNT_ID);
    }

    @Test
    void recommendationPipelineAddsFallbackReasonWhenNoScoringRuleMatches() {
        FundingListing oldEquityListing = listing(3L, FundingModel.EQUITY, NOW.minusSeconds(10L * 24 * 60 * 60));
        RecommendationRequest request = new RecommendationRequest(
                ACCOUNT_ID,
                FundingModel.EQUITY,
                null,
                null,
                null,
                1,
                20,
                50
        );

        when(investorPreferenceQueryPort.getMyPreferences(ACCOUNT_ID))
                .thenReturn(new InvestorPreferenceResponse(10L, List.of()));
        when(listingRepository.findOpenListings(
                eq(FundingModel.EQUITY),
                eq(null),
                eq(null),
                eq(null),
                eq(ListingSortMode.NEWEST),
                eq(PageRequest.of(0, 50))
        )).thenReturn(new PageImpl<>(List.of(oldEquityListing)));

        List<RecommendationCandidate> recommendations = pipeline.recommend(request);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).score()).isZero();
        assertThat(recommendations.get(0).toRecommendationInfo().reasons())
                .containsExactly("Open listing available for bidding");
    }

    private static FundingListing listing(Long startupId, FundingModel fundingModel, Instant publishedAt) {
        return FundingListing.builder()
                .listingId(startupId * 100)
                .startupId(startupId)
                .fundingModel(fundingModel)
                .listingState(ListingState.OPEN)
                .title("Recommendation listing " + startupId)
                .fundingPurposeDescription("Funds needed for marketplace recommendation testing.")
                .createdAt(publishedAt.minusSeconds(3_600))
                .publishedAt(publishedAt)
                .expiresAt(publishedAt.plusSeconds(14L * 24 * 60 * 60))
                .debtTerms(ListingDebtTerms.create(
                        new BigDecimal("550000.00"),
                        "INR",
                        new BigDecimal("9.50"),
                        new BigDecimal("12.75"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        publishedAt.minusSeconds(3_600)
                ))
                .build();
    }
}

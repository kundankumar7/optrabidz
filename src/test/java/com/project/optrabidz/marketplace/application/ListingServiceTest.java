package com.project.optrabidz.marketplace.application;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.governance.application.constraint.EligibilityEvaluationController;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.application.dto.request.ListingDebtTermsRequest;
import com.project.optrabidz.marketplace.application.dto.request.UpdateListingRequest;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;
import com.project.optrabidz.marketplace.application.exception.ListingNotFoundException;
import com.project.optrabidz.marketplace.application.factory.FundingListingFactory;
import com.project.optrabidz.marketplace.application.policy.FundingModelPolicyResolver;
import com.project.optrabidz.marketplace.application.policy.ListingExpiryPolicy;
import com.project.optrabidz.marketplace.application.specification.ListingCanBeClosedSpec;
import com.project.optrabidz.marketplace.application.specification.ListingCanBePublishedSpec;
import com.project.optrabidz.marketplace.application.specification.ListingCanBeUpdatedSpec;
import com.project.optrabidz.marketplace.application.specification.ListingVisibleToActorSpec;
import com.project.optrabidz.marketplace.application.specification.StartupOwnsListingSpec;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingDebtTerms;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {
    private static final Long ACCOUNT_ID = 101L;
    private static final Long STARTUP_ID = 11L;
    private static final Long LISTING_ID = 201L;
    private static final Instant NOW = Instant.parse("2026-08-22T12:00:00Z");

    @Mock
    private FundingListingRepository listingRepository;
    @Mock
    private StartupRepository startupRepository;
    @Mock
    private FundingListingFactory listingFactory;
    @Mock
    private FundingModelPolicyResolver policyResolver;
    @Mock
    private ListingExpiryPolicy listingExpiryPolicy;
    @Mock
    private EligibilityEvaluationController eligibilityEvaluationController;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private MarketplaceResponseMapper responseMapper;
    @Mock
    private ListingCanBeUpdatedSpec listingCanBeUpdatedSpec;
    @Mock
    private ListingCanBePublishedSpec listingCanBePublishedSpec;
    @Mock
    private ListingCanBeClosedSpec listingCanBeClosedSpec;
    @Mock
    private StartupOwnsListingSpec startupOwnsListingSpec;
    @Mock
    private ListingVisibleToActorSpec listingVisibleToActorSpec;

    @InjectMocks
    private ListingService service;

    @Test
    void missingListingUsesApprovedNotFoundDescriptor() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getListingDetails(LISTING_ID, null, null))
                .isInstanceOf(ListingNotFoundException.class)
                .satisfies(failure -> org.assertj.core.api.Assertions.assertThat(
                                ((ApplicationException) failure).descriptor())
                        .isSameAs(MarketplaceErrors.LISTING_NOT_FOUND));
    }

    @Test
    void unexpectedAggregateInvariantIsNotRelabeledAsListingConflict() {
        FundingListing openListing = openListing();
        when(startupRepository.findByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(startup()));
        when(listingRepository.findById(LISTING_ID))
                .thenReturn(Optional.of(openListing));

        assertThatThrownBy(() -> service.updateDraftListing(
                ACCOUNT_ID,
                RoleType.STARTUP,
                LISTING_ID,
                updateRequest()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only DRAFT listings can be updated");

        verify(listingRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void unsupportedFundingModelUsesApprovedBusinessRuleDescriptor() {
        FundingModelPolicyResolver resolver = new FundingModelPolicyResolver(List.of());

        assertThatThrownBy(() -> resolver.resolve(FundingModel.EQUITY))
                .isInstanceOf(ApplicationException.class)
                .satisfies(failure -> org.assertj.core.api.Assertions.assertThat(
                                ((ApplicationException) failure).descriptor())
                        .isSameAs(MarketplaceErrors.UNSUPPORTED_FUNDING_MODEL));
    }

    private static FundingListing openListing() {
        return FundingListing.builder()
                .listingId(LISTING_ID)
                .startupId(STARTUP_ID)
                .fundingModel(FundingModel.DEBT)
                .listingState(ListingState.OPEN)
                .title("Working capital listing")
                .fundingPurposeDescription("Funds needed for inventory expansion.")
                .createdAt(NOW.minusSeconds(120))
                .publishedAt(NOW.minusSeconds(60))
                .expiresAt(NOW.plusSeconds(3600))
                .debtTerms(ListingDebtTerms.create(
                        new BigDecimal("550000.00"),
                        "INR",
                        new BigDecimal("9.50"),
                        new BigDecimal("12.75"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        NOW.minusSeconds(120)
                ))
                .build();
    }

    private static Startup startup() {
        return new Startup(
                STARTUP_ID,
                ACCOUNT_ID,
                "Startup One Private Limited",
                "IN",
                "Startup One",
                "Helps startups manage fundraising workflows.",
                List.of("https://startupone.example.com"),
                List.of()
        );
    }

    private static UpdateListingRequest updateRequest() {
        return new UpdateListingRequest(
                "Updated listing",
                "Updated funding purpose for business expansion.",
                new ListingDebtTermsRequest(
                        new BigDecimal("560000.00"),
                        "INR",
                        new BigDecimal("9.75"),
                        new BigDecimal("12.50"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null
                )
        );
    }
}

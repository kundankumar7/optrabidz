package com.project.optrabidz.marketplace.application.specification;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.application.exception.BidAlreadyAcceptedException;
import com.project.optrabidz.marketplace.application.exception.InvalidBidStateException;
import com.project.optrabidz.marketplace.application.exception.InvalidListingStateException;
import com.project.optrabidz.marketplace.application.exception.MarketplaceAccessException;
import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.BidDebtTerms;
import com.project.optrabidz.marketplace.domain.model.BidState;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingDebtTerms;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import com.project.optrabidz.participation.domain.model.Startup;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketplaceSpecificationTest {
    private static final Instant NOW = Instant.parse("2026-05-24T08:00:00Z");

    @Test
    void listingLifecycleSpecsRejectInvalidStates() {
        FundingListing draft = listing(ListingState.DRAFT, null);
        FundingListing open = listing(ListingState.OPEN, null);

        assertThatNoException().isThrownBy(() -> new ListingCanBeUpdatedSpec().assertSatisfiedBy(draft));
        assertThatNoException().isThrownBy(() -> new ListingCanBePublishedSpec().assertSatisfiedBy(draft));
        assertThatNoException().isThrownBy(() -> new ListingCanBeClosedSpec().assertSatisfiedBy(open));

        assertThatThrownBy(() -> new ListingCanBeUpdatedSpec().assertSatisfiedBy(open))
                .isInstanceOf(InvalidListingStateException.class)
                .hasMessageContaining("Only DRAFT listings can be updated");
        assertThatThrownBy(() -> new ListingCanBePublishedSpec().assertSatisfiedBy(open))
                .isInstanceOf(InvalidListingStateException.class)
                .hasMessageContaining("Only DRAFT listings can be published");
        assertThatThrownBy(() -> new ListingCanBeClosedSpec().assertSatisfiedBy(draft))
                .isInstanceOf(InvalidListingStateException.class)
                .hasMessageContaining("Only OPEN listings can be closed");
    }

    @Test
    void bidAcceptanceSpecRejectsClosedListingAcceptedListingAndNonSubmittedBid() {
        BidCanBeAcceptedSpec spec = new BidCanBeAcceptedSpec();
        FundingListing openListing = listing(ListingState.OPEN, null);
        Bid submittedBid = bid(BidState.SUBMITTED);

        assertThatNoException().isThrownBy(() -> spec.assertSatisfiedBy(openListing, submittedBid, false));

        assertThatThrownBy(() -> spec.assertSatisfiedBy(listing(ListingState.CLOSED, NOW), submittedBid, false))
                .isInstanceOf(BidAlreadyAcceptedException.class)
                .hasMessageContaining("Listing is not open for bid acceptance");
        assertThatThrownBy(() -> spec.assertSatisfiedBy(openListing, submittedBid, true))
                .isInstanceOf(BidAlreadyAcceptedException.class)
                .hasMessageContaining("Listing already has an accepted bid");
        assertThatThrownBy(() -> spec.assertSatisfiedBy(openListing, bid(BidState.WITHDRAWN), false))
                .isInstanceOf(InvalidBidStateException.class)
                .hasMessageContaining("Only SUBMITTED bids can be accepted");
    }

    @Test
    void ownershipAndVisibilitySpecsProtectPrivateMarketplaceResources() {
        FundingListing listing = listing(ListingState.AGREEMENT_REACHED, NOW);
        Startup owner = startup(11L, 101L);
        Startup outsider = startup(12L, 202L);

        assertThatNoException().isThrownBy(() -> new StartupOwnsListingSpec().assertSatisfiedBy(owner, listing));
        assertThatThrownBy(() -> new StartupOwnsListingSpec().assertSatisfiedBy(outsider, listing))
                .isInstanceOf(MarketplaceAccessException.class)
                .hasMessageContaining("Startup can access only owned listings");

        assertThatNoException().isThrownBy(() -> new ListingVisibleToActorSpec()
                .assertSatisfiedBy(listing, owner.getAccountId(), RoleType.STARTUP, owner));
        assertThatNoException().isThrownBy(() -> new ListingVisibleToActorSpec()
                .assertSatisfiedBy(listing, 999L, RoleType.ADMIN, owner));
        assertThatThrownBy(() -> new ListingVisibleToActorSpec()
                .assertSatisfiedBy(listing, outsider.getAccountId(), RoleType.STARTUP, owner))
                .isInstanceOf(MarketplaceAccessException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void investorCannotBidOnOwnStartupListing() {
        Startup ownStartup = startup(11L, 220L);

        assertThatNoException().isThrownBy(() -> new InvestorCannotBidOnOwnListingSpec()
                .assertSatisfiedBy(221L, ownStartup));
        assertThatThrownBy(() -> new InvestorCannotBidOnOwnListingSpec()
                .assertSatisfiedBy(ownStartup.getAccountId(), ownStartup))
                .isInstanceOf(MarketplaceAccessException.class)
                .hasMessageContaining("Investor cannot bid on own startup listing");
    }

    private static FundingListing listing(ListingState state, Instant closedAt) {
        return FundingListing.builder()
                .listingId(101L)
                .startupId(11L)
                .fundingModel(FundingModel.DEBT)
                .listingState(state)
                .title("Working capital listing")
                .fundingPurposeDescription("Funds needed for inventory expansion.")
                .createdAt(NOW.minusSeconds(120))
                .publishedAt(state == ListingState.DRAFT ? null : NOW.minusSeconds(60))
                .expiresAt(state == ListingState.DRAFT ? null : NOW.plusSeconds(3_600))
                .closedAt(closedAt)
                .debtTerms(listingDebtTerms())
                .build();
    }

    private static ListingDebtTerms listingDebtTerms() {
        return ListingDebtTerms.create(
                new BigDecimal("550000.00"),
                "INR",
                new BigDecimal("9.50"),
                new BigDecimal("12.75"),
                18,
                RepaymentPlanType.INSTALLMENT_MONTHLY,
                null,
                NOW.minusSeconds(120)
        );
    }

    private static Bid bid(BidState state) {
        return Bid.builder()
                .bidId(501L)
                .listingId(101L)
                .investorId(22L)
                .fundingModel(FundingModel.DEBT)
                .bidState(state)
                .proposalMessage("We are interested in funding this listing.")
                .createdAt(NOW.minusSeconds(30))
                .debtTerms(BidDebtTerms.create(
                        new BigDecimal("500000.00"),
                        new BigDecimal("10.50"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        NOW.minusSeconds(30)
                ))
                .build();
    }

    private static Startup startup(Long startupId, Long accountId) {
        return new Startup(
                startupId,
                accountId,
                "Startup One Private Limited",
                "IN",
                "Startup One",
                "Helps startups manage fundraising workflows.",
                List.of("https://startupone.example.com"),
                List.of()
        );
    }
}

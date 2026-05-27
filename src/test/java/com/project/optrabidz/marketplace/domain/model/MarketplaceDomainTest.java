package com.project.optrabidz.marketplace.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketplaceDomainTest {
    private static final Instant NOW = Instant.parse("2026-05-19T10:00:00Z");

    @Test
    void draftListingCanBePublishedAndClosed() {
        FundingListing listing = draftListing();
        Instant publishedAt = NOW.plusSeconds(60);
        Instant expiresAt = publishedAt.plusSeconds(1_200);

        listing.publish(publishedAt, expiresAt);

        assertThat(listing.getListingState()).isEqualTo(ListingState.OPEN);
        assertThat(listing.isOpen()).isTrue();
        assertThat(listing.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(listing.getExpiresAt()).isEqualTo(expiresAt);

        Instant closedAt = expiresAt.minusSeconds(60);
        listing.close(closedAt);

        assertThat(listing.getListingState()).isEqualTo(ListingState.CLOSED);
        assertThat(listing.getClosedAt()).isEqualTo(closedAt);
        assertThat(listing.isOpen()).isFalse();
    }

    @Test
    void listingCannotBePublishedWhenExpiryIsNotAfterPublishTime() {
        FundingListing listing = draftListing();

        assertThatThrownBy(() -> listing.publish(NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt must be after publishedAt");
    }

    @Test
    void onlyOpenListingCanReachAgreement() {
        FundingListing listing = draftListing();

        assertThatThrownBy(() -> listing.markAgreementReached(NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only OPEN listings can reach agreement");

        listing.publish(NOW, NOW.plusSeconds(600));
        listing.markAgreementReached(NOW.plusSeconds(120));

        assertThat(listing.getListingState()).isEqualTo(ListingState.AGREEMENT_REACHED);
        assertThat(listing.getClosedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void debtListingTermsValidateAndNormalizeCurrencyCode() {
        ListingDebtTerms terms = ListingDebtTerms.create(
                new BigDecimal("550000.00"),
                " inr ",
                new BigDecimal("9.50"),
                new BigDecimal("12.75"),
                18,
                RepaymentPlanType.INSTALLMENT_MONTHLY,
                null,
                NOW
        );

        assertThat(terms.getCurrencyCode()).isEqualTo("INR");
        assertThat(terms.getRequestedAmount()).isEqualByComparingTo("550000.00");
        assertThat(terms.getRepaymentPlanType()).isEqualTo(RepaymentPlanType.INSTALLMENT_MONTHLY);
        assertThat(terms.getOneTimeRepaymentDueAfterMonths()).isNull();
    }

    @Test
    void oneTimeDebtListingTermsRequireDueAfterMonthsWithinTenure() {
        ListingDebtTerms terms = ListingDebtTerms.create(
                new BigDecimal("550000.00"),
                "INR",
                new BigDecimal("9.50"),
                new BigDecimal("12.75"),
                18,
                RepaymentPlanType.ONE_TIME,
                1,
                NOW
        );

        assertThat(terms.getRepaymentPlanType()).isEqualTo(RepaymentPlanType.ONE_TIME);
        assertThat(terms.getOneTimeRepaymentDueAfterMonths()).isEqualTo(1);

        assertThatThrownBy(() -> ListingDebtTerms.create(
                new BigDecimal("550000.00"),
                "INR",
                new BigDecimal("9.50"),
                new BigDecimal("12.75"),
                18,
                RepaymentPlanType.ONE_TIME,
                null,
                NOW
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oneTimeRepaymentDueAfterMonths must not be null");
    }

    @Test
    void debtListingTermsRejectInvalidInterestRange() {
        assertThatThrownBy(() -> ListingDebtTerms.create(
                new BigDecimal("100000.00"),
                "INR",
                new BigDecimal("14.00"),
                new BigDecimal("11.00"),
                12,
                RepaymentPlanType.INSTALLMENT_MONTHLY,
                null,
                NOW
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimumInterestRate must be less than or equal to maximumInterestRate");
    }

    @Test
    void submittedBidCanBeAcceptedButCannotBeWithdrawnAfterAcceptance() {
        Bid bid = submittedBid();
        Instant acceptedAt = NOW.plusSeconds(90);

        bid.accept(acceptedAt);

        assertThat(bid.getBidState()).isEqualTo(BidState.ACCEPTED);
        assertThat(bid.getAcceptedAt()).isEqualTo(acceptedAt);

        assertThatThrownBy(() -> bid.withdraw(NOW.plusSeconds(120)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only SUBMITTED bids can be withdrawn");
    }

    @Test
    void submittedBidCanBeWithdrawnOrRejectedOnlyOnce() {
        Bid withdrawnBid = submittedBid();
        withdrawnBid.withdraw(NOW.plusSeconds(30));

        assertThat(withdrawnBid.getBidState()).isEqualTo(BidState.WITHDRAWN);
        assertThat(withdrawnBid.getWithdrawnAt()).isEqualTo(NOW.plusSeconds(30));

        Bid rejectedBid = submittedBid();
        rejectedBid.reject(NOW.plusSeconds(45));

        assertThat(rejectedBid.getBidState()).isEqualTo(BidState.REJECTED);
        assertThat(rejectedBid.getRejectedAt()).isEqualTo(NOW.plusSeconds(45));
    }

    @Test
    void agreementCanBeCreatedOnlyFromAcceptedBid() {
        FundingListing listing = persistedOpenListing();
        Bid submittedBid = persistedBid(BidState.SUBMITTED);

        assertThatThrownBy(() -> Agreement.fromAcceptedBid(listing, submittedBid, 11L, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agreement can be created only from accepted bid");

        Bid acceptedBid = persistedBid(BidState.ACCEPTED);
        Agreement agreement = Agreement.fromAcceptedBid(listing, acceptedBid, 11L, NOW);

        assertThat(agreement.getListingId()).isEqualTo(101L);
        assertThat(agreement.getBidId()).isEqualTo(501L);
        assertThat(agreement.getStartupId()).isEqualTo(11L);
        assertThat(agreement.getInvestorId()).isEqualTo(22L);
        assertThat(agreement.getFundingModel()).isEqualTo(FundingModel.DEBT);
        assertThat(agreement.getDebtTerms().getPrincipalAmount()).isEqualByComparingTo("500000.00");
        assertThat(agreement.getDebtTerms().getInterestRate()).isEqualByComparingTo("10.50");
    }

    private static FundingListing draftListing() {
        return FundingListing.createDraft(
                11L,
                FundingModel.DEBT,
                "Working capital listing",
                "Funds needed for inventory expansion.",
                listingDebtTerms(),
                NOW
        );
    }

    private static FundingListing persistedOpenListing() {
        return FundingListing.builder()
                .listingId(101L)
                .startupId(11L)
                .fundingModel(FundingModel.DEBT)
                .listingState(ListingState.OPEN)
                .title("Working capital listing")
                .fundingPurposeDescription("Funds needed for inventory expansion.")
                .createdAt(NOW)
                .publishedAt(NOW.plusSeconds(10))
                .expiresAt(NOW.plusSeconds(1_000))
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
                NOW
        );
    }

    private static Bid submittedBid() {
        return Bid.submit(
                101L,
                22L,
                FundingModel.DEBT,
                bidDebtTerms(),
                "We are interested in funding this listing.",
                NOW
        );
    }

    private static Bid persistedBid(BidState bidState) {
        return Bid.builder()
                .bidId(501L)
                .listingId(101L)
                .investorId(22L)
                .fundingModel(FundingModel.DEBT)
                .bidState(bidState)
                .proposalMessage("We are interested in funding this listing.")
                .createdAt(NOW)
                .debtTerms(bidDebtTerms())
                .build();
    }

    private static BidDebtTerms bidDebtTerms() {
        return BidDebtTerms.create(
                new BigDecimal("500000.00"),
                new BigDecimal("10.50"),
                18,
                RepaymentPlanType.INSTALLMENT_MONTHLY,
                null,
                NOW
        );
    }
}

package com.project.optrabidz.marketplace.application;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.governance.application.common.GovernanceDecision;
import com.project.optrabidz.governance.application.constraint.CrossLifecycleConstraintController;
import com.project.optrabidz.governance.application.constraint.EligibilityEvaluationController;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.application.dto.request.BidActionRequest;
import com.project.optrabidz.marketplace.application.dto.request.BidDebtTermsRequest;
import com.project.optrabidz.marketplace.application.dto.request.SubmitBidRequest;
import com.project.optrabidz.marketplace.application.dto.response.AcceptBidResponse;
import com.project.optrabidz.marketplace.application.dto.response.BidResponse;
import com.project.optrabidz.marketplace.application.exception.BidAlreadyAcceptedException;
import com.project.optrabidz.marketplace.application.factory.AgreementFactory;
import com.project.optrabidz.marketplace.application.factory.BidFactory;
import com.project.optrabidz.marketplace.application.factory.DebtTermsFactory;
import com.project.optrabidz.marketplace.application.policy.DebtFundingModelPolicy;
import com.project.optrabidz.marketplace.application.policy.FundingModelPolicyResolver;
import com.project.optrabidz.marketplace.application.port.FinanceAgreementPort;
import com.project.optrabidz.marketplace.application.specification.BidCanBeAcceptedSpec;
import com.project.optrabidz.marketplace.application.specification.BidCanBeRejectedSpec;
import com.project.optrabidz.marketplace.application.specification.BidCanBeSubmittedSpec;
import com.project.optrabidz.marketplace.application.specification.BidCanBeWithdrawnSpec;
import com.project.optrabidz.marketplace.application.specification.BidVisibleToActorSpec;
import com.project.optrabidz.marketplace.application.specification.InvestorCannotBidOnOwnListingSpec;
import com.project.optrabidz.marketplace.application.specification.InvestorOwnsBidSpec;
import com.project.optrabidz.marketplace.application.specification.StartupOwnsListingSpec;
import com.project.optrabidz.marketplace.domain.model.Agreement;
import com.project.optrabidz.marketplace.domain.model.Bid;
import com.project.optrabidz.marketplace.domain.model.BidDebtTerms;
import com.project.optrabidz.marketplace.domain.model.BidState;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingDebtTerms;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import com.project.optrabidz.marketplace.domain.model.RepaymentPlanType;
import com.project.optrabidz.marketplace.domain.repository.AgreementRepository;
import com.project.optrabidz.marketplace.domain.repository.BidRepository;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import com.project.optrabidz.participation.domain.model.Investor;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {
    private static final Long STARTUP_ACCOUNT_ID = 110L;
    private static final Long INVESTOR_ACCOUNT_ID = 220L;
    private static final Long STARTUP_ID = 11L;
    private static final Long INVESTOR_ID = 22L;
    private static final Long LISTING_ID = 101L;
    private static final Long BID_ID = 501L;
    private static final Long AGREEMENT_ID = 701L;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private FundingListingRepository listingRepository;

    @Mock
    private AgreementRepository agreementRepository;

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private StartupRepository startupRepository;

    @Mock
    private EligibilityEvaluationController eligibilityEvaluationController;

    @Mock
    private CrossLifecycleConstraintController crossLifecycleConstraintController;

    @Mock
    private FinanceAgreementPort financeAgreementPort;

    @Mock
    private EventPublisher eventPublisher;

    private BidService service;

    @BeforeEach
    void setUp() {
        DebtTermsFactory debtTermsFactory = new DebtTermsFactory();
        BidFactory bidFactory = new BidFactory(debtTermsFactory);
        AgreementFactory agreementFactory = new AgreementFactory();
        FundingModelPolicyResolver policyResolver = new FundingModelPolicyResolver(List.of(new DebtFundingModelPolicy()));
        MarketplaceResponseMapper responseMapper = new MarketplaceResponseMapper();

        service = new BidService(
                bidRepository,
                listingRepository,
                agreementRepository,
                investorRepository,
                startupRepository,
                bidFactory,
                agreementFactory,
                policyResolver,
                eligibilityEvaluationController,
                crossLifecycleConstraintController,
                financeAgreementPort,
                eventPublisher,
                responseMapper,
                new BidCanBeSubmittedSpec(),
                new BidCanBeWithdrawnSpec(),
                new BidCanBeRejectedSpec(),
                new BidCanBeAcceptedSpec(),
                new StartupOwnsListingSpec(),
                new InvestorOwnsBidSpec(),
                new InvestorCannotBidOnOwnListingSpec(),
                new BidVisibleToActorSpec()
        );
    }

    @Test
    void investorCanSubmitBidForOpenDebtListing() {
        when(investorRepository.findByAccountId(INVESTOR_ACCOUNT_ID)).thenReturn(Optional.of(investor()));
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(openListing()));
        when(startupRepository.findById(STARTUP_ID)).thenReturn(Optional.of(startup()));
        when(crossLifecycleConstraintController.evaluateBidSubmission(ListingState.OPEN.name()))
                .thenReturn(GovernanceDecision.allow("Listing can accept bids"));
        when(bidRepository.existsActiveByInvestorIdAndListingId(INVESTOR_ID, LISTING_ID)).thenReturn(false);
        when(bidRepository.save(any(Bid.class)))
                .thenAnswer(invocation -> withBidId(invocation.getArgument(0), BID_ID));

        BidResponse response = service.submitBid(INVESTOR_ACCOUNT_ID, RoleType.INVESTOR, submitBidRequest());

        assertThat(response.bidId()).isEqualTo(BID_ID);
        assertThat(response.listingId()).isEqualTo(LISTING_ID);
        assertThat(response.fundingModel()).isEqualTo(FundingModel.DEBT);
        assertThat(response.bidState()).isEqualTo(BidState.SUBMITTED);
        assertThat(response.debtTerms().proposedAmount()).isEqualByComparingTo("500000.00");
        verify(eligibilityEvaluationController).assertInvestorCanSubmitBid(INVESTOR_ACCOUNT_ID);
        verify(eventPublisher).publish(any());
    }

    @Test
    void startupAcceptsBidCreatesAgreementAndNotifiesFinance() {
        FundingListing listing = openListing();
        Bid bid = submittedBid();
        when(startupRepository.findByAccountId(STARTUP_ACCOUNT_ID)).thenReturn(Optional.of(startup()));
        when(bidRepository.findById(BID_ID)).thenReturn(Optional.of(bid));
        when(listingRepository.findById(LISTING_ID))
                .thenReturn(Optional.of(listing), Optional.of(agreementReachedListing()));
        when(bidRepository.existsAcceptedByListingId(LISTING_ID)).thenReturn(false);
        when(crossLifecycleConstraintController.evaluateAgreementCreation(BidState.ACCEPTED.name()))
                .thenReturn(GovernanceDecision.allow("Accepted bid can create agreement"));
        when(listingRepository.markAgreementReachedIfOpen(any(Long.class), any(Instant.class))).thenReturn(1);
        when(bidRepository.saveAndFlush(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agreementRepository.save(any(Agreement.class)))
                .thenAnswer(invocation -> withAgreementId(invocation.getArgument(0), AGREEMENT_ID));
        when(startupRepository.findById(STARTUP_ID)).thenReturn(Optional.of(startup()));
        when(investorRepository.findById(INVESTOR_ID)).thenReturn(Optional.of(investor()));

        AcceptBidResponse response = service.acceptBid(
                STARTUP_ACCOUNT_ID,
                RoleType.STARTUP,
                BID_ID,
                new BidActionRequest("Looks good", "CONFIRM")
        );

        assertThat(response.bid().bidState()).isEqualTo(BidState.ACCEPTED);
        assertThat(response.listing().listingState()).isEqualTo(ListingState.AGREEMENT_REACHED);
        assertThat(response.agreement().agreementId()).isEqualTo(AGREEMENT_ID);
        assertThat(response.agreement().listingId()).isEqualTo(LISTING_ID);
        assertThat(response.agreement().bidId()).isEqualTo(BID_ID);

        ArgumentCaptor<Agreement> agreementCaptor = ArgumentCaptor.forClass(Agreement.class);
        verify(financeAgreementPort).onAgreementCreated(agreementCaptor.capture());
        Agreement agreement = agreementCaptor.getValue();
        assertThat(agreement.getAgreementId()).isEqualTo(AGREEMENT_ID);
        assertThat(agreement.getStartupId()).isEqualTo(STARTUP_ID);
        assertThat(agreement.getInvestorId()).isEqualTo(INVESTOR_ID);
        assertThat(agreement.getDebtTerms().getPrincipalAmount()).isEqualByComparingTo("500000.00");
        verify(bidRepository).rejectOtherSubmittedBids(eq(LISTING_ID), eq(BID_ID), any(Instant.class));
        verify(eventPublisher, times(2)).publish(any());
    }

    @Test
    void startupCannotAcceptBidWhenListingAlreadyHasAcceptedBid() {
        when(startupRepository.findByAccountId(STARTUP_ACCOUNT_ID)).thenReturn(Optional.of(startup()));
        when(bidRepository.findById(BID_ID)).thenReturn(Optional.of(submittedBid()));
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(openListing()));
        when(bidRepository.existsAcceptedByListingId(LISTING_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.acceptBid(
                STARTUP_ACCOUNT_ID,
                RoleType.STARTUP,
                BID_ID,
                new BidActionRequest("Looks good", "CONFIRM")
        ))
                .isInstanceOf(BidAlreadyAcceptedException.class)
                .hasMessageContaining("Listing already has an accepted bid");

        verify(listingRepository, never()).markAgreementReachedIfOpen(any(Long.class), any(Instant.class));
        verify(listingRepository, never()).save(any());
        verify(bidRepository, never()).saveAndFlush(any());
        verify(bidRepository, never()).rejectOtherSubmittedBids(any(), any(), any());
        verify(agreementRepository, never()).save(any());
        verify(financeAgreementPort, never()).onAgreementCreated(any());
    }

    private static SubmitBidRequest submitBidRequest() {
        return new SubmitBidRequest(
                LISTING_ID,
                FundingModel.DEBT,
                new BidDebtTermsRequest(
                        new BigDecimal("500000.00"),
                        new BigDecimal("10.50"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null
                ),
                "We are interested in funding this listing."
        );
    }

    private static FundingListing openListing() {
        return FundingListing.builder()
                .listingId(LISTING_ID)
                .startupId(STARTUP_ID)
                .fundingModel(FundingModel.DEBT)
                .listingState(ListingState.OPEN)
                .title("Working capital listing")
                .fundingPurposeDescription("Funds needed for inventory expansion.")
                .createdAt(now().minusSeconds(120))
                .publishedAt(now().minusSeconds(60))
                .expiresAt(now().plusSeconds(3_600))
                .debtTerms(ListingDebtTerms.create(
                        new BigDecimal("550000.00"),
                        "INR",
                        new BigDecimal("9.50"),
                        new BigDecimal("12.75"),
                        18,
                        RepaymentPlanType.INSTALLMENT_MONTHLY,
                        null,
                        now().minusSeconds(120)
                ))
                .build();
    }

    private static FundingListing agreementReachedListing() {
        FundingListing listing = openListing();
        listing.markAgreementReached(now());
        return listing;
    }

    private static Bid submittedBid() {
        return Bid.builder()
                .bidId(BID_ID)
                .listingId(LISTING_ID)
                .investorId(INVESTOR_ID)
                .fundingModel(FundingModel.DEBT)
                .bidState(BidState.SUBMITTED)
                .proposalMessage("We are interested in funding this listing.")
                .createdAt(now().minusSeconds(30))
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
                now().minusSeconds(30)
        );
    }

    private static Startup startup() {
        return new Startup(
                STARTUP_ID,
                STARTUP_ACCOUNT_ID,
                "Startup One Private Limited",
                "IN",
                "Startup One",
                "Helps startups manage fundraising workflows.",
                List.of("https://startupone.example.com"),
                List.of()
        );
    }

    private static Investor investor() {
        return new Investor(
                INVESTOR_ID,
                INVESTOR_ACCOUNT_ID,
                "Investor One",
                "Early-stage investor focused on SaaS and fintech.",
                "Investor One Ventures LLP",
                List.of("https://investorone.example.com")
        );
    }

    private static Bid withBidId(Bid bid, Long bidId) {
        return Bid.builder()
                .bidId(bidId)
                .listingId(bid.getListingId())
                .investorId(bid.getInvestorId())
                .fundingModel(bid.getFundingModel())
                .bidState(bid.getBidState())
                .proposalMessage(bid.getProposalMessage())
                .createdAt(bid.getCreatedAt())
                .withdrawnAt(bid.getWithdrawnAt())
                .rejectedAt(bid.getRejectedAt())
                .acceptedAt(bid.getAcceptedAt())
                .fundedAt(bid.getFundedAt())
                .debtTerms(bid.getDebtTerms())
                .build();
    }

    private static Agreement withAgreementId(Agreement agreement, Long agreementId) {
        return Agreement.builder()
                .agreementId(agreementId)
                .listingId(agreement.getListingId())
                .bidId(agreement.getBidId())
                .startupId(agreement.getStartupId())
                .investorId(agreement.getInvestorId())
                .fundingModel(agreement.getFundingModel())
                .createdAt(agreement.getCreatedAt())
                .debtTerms(agreement.getDebtTerms())
                .build();
    }

    private static Instant now() {
        return Instant.now();
    }
}

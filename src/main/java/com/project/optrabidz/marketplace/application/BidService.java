package com.project.optrabidz.marketplace.application;

import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.governance.application.common.GovernanceDecision;
import com.project.optrabidz.governance.application.common.GovernanceException;
import com.project.optrabidz.governance.application.constraint.CrossLifecycleConstraintController;
import com.project.optrabidz.governance.application.constraint.EligibilityEvaluationController;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.application.dto.request.BidActionRequest;
import com.project.optrabidz.marketplace.application.dto.request.SubmitBidRequest;
import com.project.optrabidz.marketplace.application.dto.response.AcceptBidResponse;
import com.project.optrabidz.marketplace.application.dto.response.AgreementResponse;
import com.project.optrabidz.marketplace.application.dto.response.BidActionResponse;
import com.project.optrabidz.marketplace.application.dto.response.BidResponse;
import com.project.optrabidz.marketplace.application.dto.response.CloseListingResponse;
import com.project.optrabidz.marketplace.application.event.AgreementCreatedEvent;
import com.project.optrabidz.marketplace.application.event.BidAcceptedEvent;
import com.project.optrabidz.marketplace.application.event.BidRejectedEvent;
import com.project.optrabidz.marketplace.application.event.BidSubmittedEvent;
import com.project.optrabidz.marketplace.application.event.BidWithdrawnEvent;
import com.project.optrabidz.marketplace.application.exception.AgreementNotFoundException;
import com.project.optrabidz.marketplace.application.exception.BidAlreadyAcceptedException;
import com.project.optrabidz.marketplace.application.exception.BidAlreadyExistsException;
import com.project.optrabidz.marketplace.application.exception.BidNotFoundException;
import com.project.optrabidz.marketplace.application.exception.InvalidBidStateException;
import com.project.optrabidz.marketplace.application.exception.MarketplaceAccessException;
import com.project.optrabidz.marketplace.application.factory.AgreementFactory;
import com.project.optrabidz.marketplace.application.factory.BidFactory;
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
import com.project.optrabidz.marketplace.domain.model.BidState;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.repository.AgreementRepository;
import com.project.optrabidz.marketplace.domain.repository.BidRepository;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import com.project.optrabidz.participation.application.exception.InvalidRoleException;
import com.project.optrabidz.participation.application.exception.ParticipationNotFoundException;
import com.project.optrabidz.participation.domain.model.Investor;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BidService {
    private final BidRepository bidRepository;
    private final FundingListingRepository listingRepository;
    private final AgreementRepository agreementRepository;
    private final InvestorRepository investorRepository;
    private final StartupRepository startupRepository;
    private final BidFactory bidFactory;
    private final AgreementFactory agreementFactory;
    private final FundingModelPolicyResolver policyResolver;
    private final EligibilityEvaluationController eligibilityEvaluationController;
    private final CrossLifecycleConstraintController crossLifecycleConstraintController;
    private final FinanceAgreementPort financeAgreementPort;
    private final EventPublisher eventPublisher;
    private final MarketplaceResponseMapper responseMapper;
    private final BidCanBeSubmittedSpec bidCanBeSubmittedSpec;
    private final BidCanBeWithdrawnSpec bidCanBeWithdrawnSpec;
    private final BidCanBeRejectedSpec bidCanBeRejectedSpec;
    private final BidCanBeAcceptedSpec bidCanBeAcceptedSpec;
    private final StartupOwnsListingSpec startupOwnsListingSpec;
    private final InvestorOwnsBidSpec investorOwnsBidSpec;
    private final InvestorCannotBidOnOwnListingSpec investorCannotBidOnOwnListingSpec;
    private final BidVisibleToActorSpec bidVisibleToActorSpec;

    public BidService(BidRepository bidRepository,
                      FundingListingRepository listingRepository,
                      AgreementRepository agreementRepository,
                      InvestorRepository investorRepository,
                      StartupRepository startupRepository,
                      BidFactory bidFactory,
                      AgreementFactory agreementFactory,
                      FundingModelPolicyResolver policyResolver,
                      EligibilityEvaluationController eligibilityEvaluationController,
                      CrossLifecycleConstraintController crossLifecycleConstraintController,
                      FinanceAgreementPort financeAgreementPort,
                      EventPublisher eventPublisher,
                      MarketplaceResponseMapper responseMapper,
                      BidCanBeSubmittedSpec bidCanBeSubmittedSpec,
                      BidCanBeWithdrawnSpec bidCanBeWithdrawnSpec,
                      BidCanBeRejectedSpec bidCanBeRejectedSpec,
                      BidCanBeAcceptedSpec bidCanBeAcceptedSpec,
                      StartupOwnsListingSpec startupOwnsListingSpec,
                      InvestorOwnsBidSpec investorOwnsBidSpec,
                      InvestorCannotBidOnOwnListingSpec investorCannotBidOnOwnListingSpec,
                      BidVisibleToActorSpec bidVisibleToActorSpec) {
        this.bidRepository = bidRepository;
        this.listingRepository = listingRepository;
        this.agreementRepository = agreementRepository;
        this.investorRepository = investorRepository;
        this.startupRepository = startupRepository;
        this.bidFactory = bidFactory;
        this.agreementFactory = agreementFactory;
        this.policyResolver = policyResolver;
        this.eligibilityEvaluationController = eligibilityEvaluationController;
        this.crossLifecycleConstraintController = crossLifecycleConstraintController;
        this.financeAgreementPort = financeAgreementPort;
        this.eventPublisher = eventPublisher;
        this.responseMapper = responseMapper;
        this.bidCanBeSubmittedSpec = bidCanBeSubmittedSpec;
        this.bidCanBeWithdrawnSpec = bidCanBeWithdrawnSpec;
        this.bidCanBeRejectedSpec = bidCanBeRejectedSpec;
        this.bidCanBeAcceptedSpec = bidCanBeAcceptedSpec;
        this.startupOwnsListingSpec = startupOwnsListingSpec;
        this.investorOwnsBidSpec = investorOwnsBidSpec;
        this.investorCannotBidOnOwnListingSpec = investorCannotBidOnOwnListingSpec;
        this.bidVisibleToActorSpec = bidVisibleToActorSpec;
    }

    @Transactional
    public BidResponse submitBid(Long accountId, RoleType roleType, SubmitBidRequest request) {
        ensureRole(roleType, RoleType.INVESTOR);
        Investor investor = getInvestorByAccount(accountId);
        FundingListing listing = getListing(request.listingId());
        Startup listingStartup = getStartupById(listing.getStartupId());

        eligibilityEvaluationController.assertInvestorCanSubmitBid(accountId);
        bidCanBeSubmittedSpec.assertSatisfiedBy(listing);
        assertAllowed(crossLifecycleConstraintController.evaluateBidSubmission(listing.getListingState().name()));

        investorCannotBidOnOwnListingSpec.assertSatisfiedBy(accountId, listingStartup);
        if (bidRepository.existsActiveByInvestorIdAndListingId(investor.getInvestorId(), listing.getListingId())) {
            throw new BidAlreadyExistsException("Investor already has an active bid for this listing");
        }

        Instant now = Instant.now();
        Bid bid = bidFactory.submit(investor.getInvestorId(), request, now);
        policyResolver.resolve(listing.getFundingModel()).validateBid(listing, bid);
        Bid saved = bidRepository.save(bid);
        eventPublisher.publish(new BidSubmittedEvent(saved.getBidId(), saved.getListingId(), saved.getInvestorId(), accountId, now));
        return responseMapper.toBidResponse(saved);
    }

    @Transactional(readOnly = true)
    public BidResponse getBidById(Long accountId, RoleType roleType, Long bidId) {
        Bid bid = getBid(bidId);
        ensureBidVisible(accountId, roleType, bid);
        return responseMapper.toBidResponse(bid);
    }

    @Transactional(readOnly = true)
    public PageResponse<BidResponse> getBidsForListing(Long accountId,
                                                       RoleType roleType,
                                                       Long listingId,
                                                       BidState state,
                                                       int page,
                                                       int size) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        FundingListing listing = getListing(listingId);
        startupOwnsListingSpec.assertSatisfiedBy(startup, listing);
        Pageable pageable = PageRequest.of(toPageIndex(page), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BidResponse> bids = bidRepository.findByListingId(listingId, state, pageable)
                .map(responseMapper::toBidResponse);
        return toPageResponse(bids, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<BidResponse> getMyBids(Long accountId,
                                               RoleType roleType,
                                               BidState state,
                                               int page,
                                               int size) {
        ensureRole(roleType, RoleType.INVESTOR);
        Investor investor = getInvestorByAccount(accountId);
        Pageable pageable = PageRequest.of(toPageIndex(page), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BidResponse> bids = bidRepository.findByInvestorId(investor.getInvestorId(), state, pageable)
                .map(responseMapper::toBidResponse);
        return toPageResponse(bids, page, size);
    }

    @Transactional(readOnly = true)
    public BidResponse getMyBidByListing(Long accountId, RoleType roleType, Long listingId) {
        ensureRole(roleType, RoleType.INVESTOR);
        Investor investor = getInvestorByAccount(accountId);
        return bidRepository.findLatestByInvestorIdAndListingId(investor.getInvestorId(), listingId)
                .map(responseMapper::toBidResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public BidResponse getAcceptedBid(Long accountId, RoleType roleType, Long listingId) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        FundingListing listing = getListing(listingId);
        startupOwnsListingSpec.assertSatisfiedBy(startup, listing);
        return bidRepository.findAcceptedByListingId(listingId)
                .map(responseMapper::toBidResponse)
                .orElse(null);
    }

    @Transactional
    public BidActionResponse withdrawBid(Long accountId, RoleType roleType, Long bidId, BidActionRequest request) {
        ensureRole(roleType, RoleType.INVESTOR);
        Investor investor = getInvestorByAccount(accountId);
        Bid bid = getBid(bidId);
        investorOwnsBidSpec.assertSatisfiedBy(investor, bid);
        bidCanBeWithdrawnSpec.assertSatisfiedBy(bid);
        Instant now = Instant.now();
        applyBidTransition(() -> bid.withdraw(now));
        Bid saved = bidRepository.save(bid);
        eventPublisher.publish(new BidWithdrawnEvent(
                saved.getBidId(),
                saved.getListingId(),
                saved.getInvestorId(),
                accountId,
                request == null ? null : request.reason(),
                now
        ));
        return new BidActionResponse(saved.getBidId(), saved.getBidState(), saved.getWithdrawnAt());
    }

    @Transactional
    public BidActionResponse rejectBid(Long accountId, RoleType roleType, Long bidId, BidActionRequest request) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        Bid bid = getBid(bidId);
        FundingListing listing = getListing(bid.getListingId());
        startupOwnsListingSpec.assertSatisfiedBy(startup, listing);
        bidCanBeRejectedSpec.assertSatisfiedBy(bid);
        Instant now = Instant.now();
        applyBidTransition(() -> bid.reject(now));
        Bid saved = bidRepository.save(bid);
        eventPublisher.publish(new BidRejectedEvent(
                saved.getBidId(),
                saved.getListingId(),
                saved.getInvestorId(),
                accountId,
                request == null ? null : request.reason(),
                now
        ));
        return new BidActionResponse(saved.getBidId(), saved.getBidState(), saved.getRejectedAt());
    }

    @Transactional
    public AcceptBidResponse acceptBid(Long accountId, RoleType roleType, Long bidId, BidActionRequest request) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        Bid bid = getBid(bidId);
        FundingListing listing = getListing(bid.getListingId());
        startupOwnsListingSpec.assertSatisfiedBy(startup, listing);
        bidCanBeAcceptedSpec.assertSatisfiedBy(
                listing,
                bid,
                bidRepository.existsAcceptedByListingId(listing.getListingId())
        );

        Instant now = Instant.now();
        applyBidTransition(() -> bid.accept(now));
        assertAllowed(crossLifecycleConstraintController.evaluateAgreementCreation(bid.getBidState().name()));

        try {
            int updatedListings = listingRepository.markAgreementReachedIfOpen(listing.getListingId(), now);
            if (updatedListings != 1) {
                throw new BidAlreadyAcceptedException("Listing already has an accepted bid");
            }
            FundingListing savedListing = getListing(listing.getListingId());
            Bid savedBid = bidRepository.saveAndFlush(bid);
            bidRepository.rejectOtherSubmittedBids(savedBid.getListingId(), savedBid.getBidId(), now);
            Agreement agreement = agreementFactory.createFromAcceptedBid(savedListing, savedBid, now);
            Agreement savedAgreement = agreementRepository.save(agreement);
            financeAgreementPort.onAgreementCreated(savedAgreement);
            eventPublisher.publish(new BidAcceptedEvent(
                    savedBid.getBidId(),
                    savedBid.getListingId(),
                    savedBid.getInvestorId(),
                    accountId,
                    now
            ));
            eventPublisher.publish(new AgreementCreatedEvent(
                    savedAgreement.getAgreementId(),
                    savedAgreement.getListingId(),
                    savedAgreement.getBidId(),
                    savedAgreement.getStartupId(),
                    savedAgreement.getInvestorId(),
                    accountId,
                    now
            ));
            return new AcceptBidResponse(
                    new BidActionResponse(savedBid.getBidId(), savedBid.getBidState(), savedBid.getAcceptedAt()),
                    new CloseListingResponse(savedListing.getListingId(), savedListing.getListingState(), savedListing.getClosedAt()),
                    toAgreementResponse(savedAgreement)
            );
        } catch (DataIntegrityViolationException ex) {
            throw new BidAlreadyAcceptedException("Listing already has an accepted bid");
        }
    }

    AgreementResponse toAgreementResponse(Agreement agreement) {
        Startup startup = getStartupById(agreement.getStartupId());
        Investor investor = getInvestorById(agreement.getInvestorId());
        return responseMapper.toAgreementResponse(
                agreement,
                startup.getPublicDisplayName(),
                investor.getPublicDisplayName()
        );
    }

    private void ensureBidVisible(Long accountId, RoleType roleType, Bid bid) {
        if (roleType == RoleType.INVESTOR) {
            Investor investor = getInvestorByAccount(accountId);
            bidVisibleToActorSpec.assertSatisfiedBy(roleType, bid, investor.getInvestorId(), null, null);
            return;
        }
        if (roleType == RoleType.STARTUP) {
            Startup startup = getStartupByAccount(accountId);
            FundingListing listing = getListing(bid.getListingId());
            bidVisibleToActorSpec.assertSatisfiedBy(roleType, bid, null, startup.getStartupId(), listing);
            return;
        }
        bidVisibleToActorSpec.assertSatisfiedBy(roleType, bid, null, null, null);
    }

    private FundingListing getListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new com.project.optrabidz.marketplace.application.exception.ListingNotFoundException("Funding listing not found"));
    }

    private Bid getBid(Long bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(() -> new BidNotFoundException("Bid not found"));
    }

    private Startup getStartupByAccount(Long accountId) {
        return startupRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ParticipationNotFoundException("Startup not found for this account"));
    }

    private Startup getStartupById(Long startupId) {
        return startupRepository.findById(startupId)
                .orElseThrow(() -> new ParticipationNotFoundException("Startup not found"));
    }

    private Investor getInvestorByAccount(Long accountId) {
        return investorRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ParticipationNotFoundException("Investor not found for this account"));
    }

    private Investor getInvestorById(Long investorId) {
        return investorRepository.findById(investorId)
                .orElseThrow(() -> new ParticipationNotFoundException("Investor not found"));
    }

    private void ensureRole(RoleType actualRole, RoleType expectedRole) {
        if (actualRole != expectedRole) {
            throw new InvalidRoleException("Role is not allowed to perform this operation");
        }
    }

    private void assertAllowed(GovernanceDecision decision) {
        if (!decision.allowed()) {
            throw new GovernanceException(decision);
        }
    }

    private void applyBidTransition(Runnable transition) {
        try {
            transition.run();
        } catch (IllegalStateException exception) {
            throw new InvalidBidStateException(exception.getMessage());
        }
    }

    private int toPageIndex(int page) {
        return Math.max(page, 1) - 1;
    }

    private PageResponse<BidResponse> toPageResponse(Page<BidResponse> pageData, int page, int size) {
        return new PageResponse<>(
                pageData.getContent(),
                Math.max(page, 1),
                size,
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}

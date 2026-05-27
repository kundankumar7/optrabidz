package com.project.optrabidz.marketplace.application;

import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.governance.application.constraint.EligibilityEvaluationController;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.application.dto.request.CloseListingRequest;
import com.project.optrabidz.marketplace.application.dto.request.CreateListingRequest;
import com.project.optrabidz.marketplace.application.dto.request.ListingDebtTermsRequest;
import com.project.optrabidz.marketplace.application.dto.request.PublishListingRequest;
import com.project.optrabidz.marketplace.application.dto.request.UpdateListingRequest;
import com.project.optrabidz.marketplace.application.dto.response.CloseListingResponse;
import com.project.optrabidz.marketplace.application.dto.response.ListingResponse;
import com.project.optrabidz.marketplace.application.dto.response.PublishListingResponse;
import com.project.optrabidz.marketplace.application.event.ListingClosedEvent;
import com.project.optrabidz.marketplace.application.event.ListingPublishedEvent;
import com.project.optrabidz.marketplace.application.exception.InvalidListingStateException;
import com.project.optrabidz.marketplace.application.exception.ListingNotFoundException;
import com.project.optrabidz.marketplace.application.exception.MarketplaceAccessException;
import com.project.optrabidz.marketplace.application.factory.FundingListingFactory;
import com.project.optrabidz.marketplace.application.policy.ListingExpiryPolicy;
import com.project.optrabidz.marketplace.application.policy.FundingModelPolicyResolver;
import com.project.optrabidz.marketplace.application.specification.ListingCanBeClosedSpec;
import com.project.optrabidz.marketplace.application.specification.ListingCanBePublishedSpec;
import com.project.optrabidz.marketplace.application.specification.ListingCanBeUpdatedSpec;
import com.project.optrabidz.marketplace.application.specification.ListingVisibleToActorSpec;
import com.project.optrabidz.marketplace.application.specification.StartupOwnsListingSpec;
import com.project.optrabidz.marketplace.domain.model.FundingListing;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingDebtTerms;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import com.project.optrabidz.marketplace.domain.repository.FundingListingRepository;
import com.project.optrabidz.participation.application.exception.InvalidRoleException;
import com.project.optrabidz.participation.application.exception.ParticipationNotFoundException;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ListingService {
    private final FundingListingRepository listingRepository;
    private final StartupRepository startupRepository;
    private final FundingListingFactory listingFactory;
    private final FundingModelPolicyResolver policyResolver;
    private final ListingExpiryPolicy listingExpiryPolicy;
    private final EligibilityEvaluationController eligibilityEvaluationController;
    private final EventPublisher eventPublisher;
    private final MarketplaceResponseMapper responseMapper;
    private final ListingCanBeUpdatedSpec listingCanBeUpdatedSpec;
    private final ListingCanBePublishedSpec listingCanBePublishedSpec;
    private final ListingCanBeClosedSpec listingCanBeClosedSpec;
    private final StartupOwnsListingSpec startupOwnsListingSpec;
    private final ListingVisibleToActorSpec listingVisibleToActorSpec;

    public ListingService(FundingListingRepository listingRepository,
                          StartupRepository startupRepository,
                          FundingListingFactory listingFactory,
                          FundingModelPolicyResolver policyResolver,
                          ListingExpiryPolicy listingExpiryPolicy,
                          EligibilityEvaluationController eligibilityEvaluationController,
                          EventPublisher eventPublisher,
                          MarketplaceResponseMapper responseMapper,
                          ListingCanBeUpdatedSpec listingCanBeUpdatedSpec,
                          ListingCanBePublishedSpec listingCanBePublishedSpec,
                          ListingCanBeClosedSpec listingCanBeClosedSpec,
                          StartupOwnsListingSpec startupOwnsListingSpec,
                          ListingVisibleToActorSpec listingVisibleToActorSpec) {
        this.listingRepository = listingRepository;
        this.startupRepository = startupRepository;
        this.listingFactory = listingFactory;
        this.policyResolver = policyResolver;
        this.listingExpiryPolicy = listingExpiryPolicy;
        this.eligibilityEvaluationController = eligibilityEvaluationController;
        this.eventPublisher = eventPublisher;
        this.responseMapper = responseMapper;
        this.listingCanBeUpdatedSpec = listingCanBeUpdatedSpec;
        this.listingCanBePublishedSpec = listingCanBePublishedSpec;
        this.listingCanBeClosedSpec = listingCanBeClosedSpec;
        this.startupOwnsListingSpec = startupOwnsListingSpec;
        this.listingVisibleToActorSpec = listingVisibleToActorSpec;
    }

    @Transactional
    public ListingResponse createDraftListing(Long accountId, RoleType roleType, CreateListingRequest request) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        Instant now = Instant.now();
        FundingListing listing = listingFactory.createDraft(startup.getStartupId(), request, now);
        policyResolver.resolve(listing.getFundingModel()).validateListing(listing);
        FundingListing saved = listingRepository.save(listing);
        return toListingResponse(saved);
    }

    @Transactional
    public ListingResponse updateDraftListing(Long accountId,
                                              RoleType roleType,
                                              Long listingId,
                                              UpdateListingRequest request) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        FundingListing listing = getListing(listingId);
        startupOwnsListingSpec.assertSatisfiedBy(startup, listing);
        listingCanBeUpdatedSpec.assertSatisfiedBy(listing);

        ListingDebtTermsRequest termsRequest = request.debtTerms();
        ListingDebtTerms debtTerms = listing.getDebtTerms();
        Instant now = Instant.now();
        ListingDebtTerms updatedDebtTerms = new ListingDebtTerms(
                debtTerms.getListingDebtTermsId(),
                debtTerms.getListingId(),
                termsRequest.requestedAmount(),
                termsRequest.currencyCode(),
                termsRequest.minimumInterestRate(),
                termsRequest.maximumInterestRate(),
                termsRequest.requestedTenureMonths(),
                termsRequest.repaymentPlanType(),
                termsRequest.oneTimeRepaymentDueAfterMonths(),
                debtTerms.getCreatedAt(),
                now
        );
        applyListingTransition(() -> listing.updateDraft(
                request.title(),
                request.fundingPurposeDescription(),
                updatedDebtTerms,
                now
        ));
        policyResolver.resolve(listing.getFundingModel()).validateListing(listing);
        return toListingResponse(listingRepository.save(listing));
    }

    @Transactional
    public PublishListingResponse publishListing(Long accountId,
                                                 RoleType roleType,
                                                 Long listingId,
                                                 PublishListingRequest request) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        FundingListing listing = getListing(listingId);
        startupOwnsListingSpec.assertSatisfiedBy(startup, listing);
        listingCanBePublishedSpec.assertSatisfiedBy(listing);

        eligibilityEvaluationController.assertStartupCanPublishListing(accountId);
        policyResolver.resolve(listing.getFundingModel()).validateListing(listing);

        Instant now = Instant.now();
        applyListingTransition(() -> listing.publish(now, listingExpiryPolicy.expiresAtFor(now)));
        FundingListing saved = listingRepository.save(listing);
        eventPublisher.publish(new ListingPublishedEvent(saved.getListingId(), saved.getStartupId(), accountId, now));
        return new PublishListingResponse(
                saved.getListingId(),
                saved.getListingState(),
                saved.getPublishedAt(),
                saved.getExpiresAt()
        );
    }

    @Transactional
    public CloseListingResponse closeListing(Long accountId,
                                             RoleType roleType,
                                             Long listingId,
                                             CloseListingRequest request) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        FundingListing listing = getListing(listingId);
        startupOwnsListingSpec.assertSatisfiedBy(startup, listing);
        listingCanBeClosedSpec.assertSatisfiedBy(listing);
        Instant now = Instant.now();
        applyListingTransition(() -> listing.close(now));
        FundingListing saved = listingRepository.save(listing);
        eventPublisher.publish(new ListingClosedEvent(
                saved.getListingId(),
                saved.getStartupId(),
                accountId,
                request == null ? null : request.reason(),
                now
        ));
        return new CloseListingResponse(saved.getListingId(), saved.getListingState(), saved.getClosedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> getMyListings(Long accountId,
                                                       RoleType roleType,
                                                       ListingState state,
                                                       FundingModel fundingModel,
                                                       int page,
                                                       int size) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        Pageable pageable = PageRequest.of(toPageIndex(page), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ListingResponse> listings = listingRepository.findByStartupId(
                        startup.getStartupId(),
                        state,
                        fundingModel,
                        pageable
                )
                .map(this::toListingResponse);
        return toPageResponse(listings, page, size);
    }

    @Transactional(readOnly = true)
    public ListingResponse getListingDetails(Long listingId, Long requesterAccountId, RoleType requesterRole) {
        FundingListing listing = getListing(listingId);
        if (listing.getListingState() != ListingState.OPEN) {
            Startup startup = getStartupById(listing.getStartupId());
            listingVisibleToActorSpec.assertSatisfiedBy(listing, requesterAccountId, requesterRole, startup);
        }
        return toListingResponse(listing);
    }

    FundingListing getListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException("Funding listing not found"));
    }

    ListingResponse toListingResponse(FundingListing listing) {
        Startup startup = getStartupById(listing.getStartupId());
        return responseMapper.toListingResponse(listing, startup.getPublicDisplayName(), startup.getBusinessDescription());
    }

    private Startup getStartupByAccount(Long accountId) {
        return startupRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ParticipationNotFoundException("Startup not found for this account"));
    }

    private Startup getStartupById(Long startupId) {
        return startupRepository.findById(startupId)
                .orElseThrow(() -> new ParticipationNotFoundException("Startup not found"));
    }

    private void ensureRole(RoleType actualRole, RoleType expectedRole) {
        if (actualRole != expectedRole) {
            throw new InvalidRoleException("Role is not allowed to perform this operation");
        }
    }

    private void applyListingTransition(Runnable transition) {
        try {
            transition.run();
        } catch (IllegalStateException exception) {
            throw new InvalidListingStateException(exception.getMessage());
        }
    }

    private int toPageIndex(int page) {
        return Math.max(page, 1) - 1;
    }

    private PageResponse<ListingResponse> toPageResponse(Page<ListingResponse> pageData, int page, int size) {
        return new PageResponse<>(
                pageData.getContent(),
                Math.max(page, 1),
                size,
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}


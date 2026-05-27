package com.project.optrabidz.marketplace.api;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.marketplace.application.ListingService;
import com.project.optrabidz.marketplace.application.MarketplaceDiscoveryService;
import com.project.optrabidz.marketplace.application.dto.request.CloseListingRequest;
import com.project.optrabidz.marketplace.application.dto.request.CreateListingRequest;
import com.project.optrabidz.marketplace.application.dto.request.PublishListingRequest;
import com.project.optrabidz.marketplace.application.dto.request.UpdateListingRequest;
import com.project.optrabidz.marketplace.application.dto.response.CloseListingResponse;
import com.project.optrabidz.marketplace.application.dto.response.ListingResponse;
import com.project.optrabidz.marketplace.application.dto.response.PublishListingResponse;
import com.project.optrabidz.marketplace.application.dto.response.RecommendedListingResponse;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import com.project.optrabidz.marketplace.domain.model.ListingState;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1")
public class ListingController {
    private final ListingService listingService;
    private final MarketplaceDiscoveryService marketplaceDiscoveryService;

    public ListingController(ListingService listingService,
                             MarketplaceDiscoveryService marketplaceDiscoveryService) {
        this.listingService = listingService;
        this.marketplaceDiscoveryService = marketplaceDiscoveryService;
    }

    @PostMapping("/funding-listings")
    public SuccessResponse<ListingResponse> createListing(@RequestBody @Valid CreateListingRequest request,
                                                          @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                          HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                listingService.createDraftListing(user.getAccountId(), user.getRole(), request),
                httpRequest
        );
    }

    @PatchMapping("/funding-listings/{listingId}")
    public SuccessResponse<ListingResponse> updateListing(@PathVariable Long listingId,
                                                          @RequestBody @Valid UpdateListingRequest request,
                                                          @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                          HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                listingService.updateDraftListing(user.getAccountId(), user.getRole(), listingId, request),
                httpRequest
        );
    }

    @PostMapping("/funding-listings/{listingId}/actions/publish")
    public SuccessResponse<PublishListingResponse> publishListing(@PathVariable Long listingId,
                                                                  @RequestBody(required = false) @Valid PublishListingRequest request,
                                                                  @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                                  HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                listingService.publishListing(user.getAccountId(), user.getRole(), listingId, request),
                httpRequest
        );
    }

    @PostMapping("/funding-listings/{listingId}/actions/close")
    public SuccessResponse<CloseListingResponse> closeListing(@PathVariable Long listingId,
                                                              @RequestBody(required = false) CloseListingRequest request,
                                                              @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                              HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                listingService.closeListing(user.getAccountId(), user.getRole(), listingId, request),
                httpRequest
        );
    }

    @GetMapping("/startups/me/funding-listings")
    public SuccessResponse<PageResponse<ListingResponse>> getMyListings(
            @RequestParam(required = false) ListingState state,
            @RequestParam(required = false) FundingModel fundingModel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                listingService.getMyListings(user.getAccountId(), user.getRole(), state, fundingModel, page, size),
                httpRequest
        );
    }

    @GetMapping("/funding-listings")
    public SuccessResponse<PageResponse<ListingResponse>> browseListings(
            @RequestParam(required = false) FundingModel fundingModel,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String currencyCode,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                marketplaceDiscoveryService.browseOpenListings(
                        fundingModel,
                        minAmount,
                        maxAmount,
                        currencyCode,
                        sort,
                        page,
                        size
                ),
                httpRequest
        );
    }

    @GetMapping("/funding-listings/recommended")
    public SuccessResponse<PageResponse<RecommendedListingResponse>> recommendedListings(
            @RequestParam(required = false) FundingModel fundingModel,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String currencyCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                marketplaceDiscoveryService.getRecommendedListings(
                        user.getAccountId(),
                        user.getRole(),
                        fundingModel,
                        minAmount,
                        maxAmount,
                        currencyCode,
                        page,
                        size
                ),
                httpRequest
        );
    }

    @GetMapping("/funding-listings/{listingId}")
    public SuccessResponse<ListingResponse> getListing(@PathVariable Long listingId,
                                                       @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                       HttpServletRequest httpRequest) {
        return ApiResponse.success(
                listingService.getListingDetails(
                        listingId,
                        principal == null ? null : principal.getAccountId(),
                        principal == null ? null : principal.getRole()
                ),
                httpRequest
        );
    }

    private AuthenticatedUserPrincipal requirePrincipal(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED, "Authentication is required");
        }
        return principal;
    }
}

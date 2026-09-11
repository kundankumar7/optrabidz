package com.project.optrabidz.marketplace.api;

import com.project.optrabidz.common.api.pagination.PageResponse;
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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
import java.net.URI;

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
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Funding listing created",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = ListingResponse.class)
                    ),
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = "Location",
                            description = "URI of the created funding listing",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    type = "string", format = "uri")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/ValidationProblem"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/UnauthorizedProblem"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    ref = "#/components/responses/ForbiddenProblem"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    ref = "#/components/responses/UnprocessableEntityProblem"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    ref = "#/components/responses/InternalServerProblem"
            )
    })
    public ResponseEntity<ListingResponse> createListing(
            @RequestBody @Valid CreateListingRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        ListingResponse response = listingService.createDraftListing(
                principal.getAccountId(),
                principal.getRole(),
                request
        );
        return ResponseEntity.created(
                URI.create("/api/v1/funding-listings/" + response.listingId())
        ).body(response);
    }

    @PatchMapping("/funding-listings/{listingId}")
    public ListingResponse updateListing(@PathVariable Long listingId,
                                         @RequestBody @Valid UpdateListingRequest request,
                                         @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return listingService.updateDraftListing(
                principal.getAccountId(),
                principal.getRole(),
                listingId,
                request
        );
    }

    @PostMapping("/funding-listings/{listingId}/actions/publish")
    public PublishListingResponse publishListing(
            @PathVariable Long listingId,
            @RequestBody(required = false) @Valid PublishListingRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return listingService.publishListing(
                principal.getAccountId(),
                principal.getRole(),
                listingId,
                request
        );
    }

    @PostMapping("/funding-listings/{listingId}/actions/close")
    public CloseListingResponse closeListing(
            @PathVariable Long listingId,
            @RequestBody(required = false) CloseListingRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return listingService.closeListing(
                principal.getAccountId(),
                principal.getRole(),
                listingId,
                request
        );
    }

    @GetMapping("/startups/me/funding-listings")
    public PageResponse<ListingResponse> getMyListings(
            @RequestParam(required = false) ListingState state,
            @RequestParam(required = false) FundingModel fundingModel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return listingService.getMyListings(
                principal.getAccountId(),
                principal.getRole(),
                state,
                fundingModel,
                page,
                size
        );
    }

    @GetMapping("/funding-listings")
    public PageResponse<ListingResponse> browseListings(
            @RequestParam(required = false) FundingModel fundingModel,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String currencyCode,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return marketplaceDiscoveryService.browseOpenListings(
                fundingModel,
                minAmount,
                maxAmount,
                currencyCode,
                sort,
                page,
                size
        );
    }

    @GetMapping("/funding-listings/recommended")
    public PageResponse<RecommendedListingResponse> recommendedListings(
            @RequestParam(required = false) FundingModel fundingModel,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String currencyCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return marketplaceDiscoveryService.getRecommendedListings(
                principal.getAccountId(),
                principal.getRole(),
                fundingModel,
                minAmount,
                maxAmount,
                currencyCode,
                page,
                size
        );
    }

    @GetMapping("/funding-listings/{listingId}")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Funding listing",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = ListingResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/ValidationProblem"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    ref = "#/components/responses/NotFoundProblem"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    ref = "#/components/responses/InternalServerProblem"
            )
    })
    public ListingResponse getListing(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return listingService.getListingDetails(
                listingId,
                principal == null ? null : principal.getAccountId(),
                principal == null ? null : principal.getRole()
        );
    }
}

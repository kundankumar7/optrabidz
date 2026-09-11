package com.project.optrabidz.marketplace.api;

import com.project.optrabidz.common.application.pagination.PageResponse;
import com.project.optrabidz.marketplace.application.BidService;
import com.project.optrabidz.marketplace.application.dto.request.BidActionRequest;
import com.project.optrabidz.marketplace.application.dto.request.SubmitBidRequest;
import com.project.optrabidz.marketplace.application.dto.response.AcceptBidResponse;
import com.project.optrabidz.marketplace.application.dto.response.BidActionResponse;
import com.project.optrabidz.marketplace.application.dto.response.BidResponse;
import com.project.optrabidz.marketplace.domain.model.BidState;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
public class BidController {
    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping("/bids")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Bid submitted",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = BidResponse.class)
            ),
            headers = @io.swagger.v3.oas.annotations.headers.Header(
                    name = "Location",
                    description = "URI of the created bid",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "string", format = "uri")
            )
    )
    public ResponseEntity<BidResponse> submitBid(
            @RequestBody @Valid SubmitBidRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        BidResponse response = bidService.submitBid(
                principal.getAccountId(),
                principal.getRole(),
                request
        );
        return ResponseEntity.created(
                URI.create("/api/v1/bids/" + response.bidId())
        ).body(response);
    }

    @GetMapping("/bids/{bidId}")
    public BidResponse getBid(@PathVariable Long bidId,
                              @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return bidService.getBidById(
                principal.getAccountId(),
                principal.getRole(),
                bidId
        );
    }

    @GetMapping("/bids")
    public PageResponse<BidResponse> getBidsForListing(
            @RequestParam Long listingId,
            @RequestParam(required = false) BidState state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return bidService.getBidsForListing(
                principal.getAccountId(),
                principal.getRole(),
                listingId,
                state,
                page,
                size
        );
    }

    @GetMapping("/investors/me/bids")
    public PageResponse<BidResponse> getMyBids(
            @RequestParam(required = false) BidState state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return bidService.getMyBids(
                principal.getAccountId(),
                principal.getRole(),
                state,
                page,
                size
        );
    }

    @GetMapping("/investors/me/bids/by-listing/{listingId}")
    public BidResponse getMyBidByListing(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return bidService.getMyBidByListing(
                principal.getAccountId(),
                principal.getRole(),
                listingId
        );
    }

    @GetMapping("/funding-listings/{listingId}/accepted-bid")
    public BidResponse getAcceptedBid(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return bidService.getAcceptedBid(
                principal.getAccountId(),
                principal.getRole(),
                listingId
        );
    }

    @PostMapping("/bids/{bidId}/actions/withdraw")
    public BidActionResponse withdrawBid(
            @PathVariable Long bidId,
            @RequestBody(required = false) BidActionRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return bidService.withdrawBid(
                principal.getAccountId(),
                principal.getRole(),
                bidId,
                request
        );
    }

    @PostMapping("/bids/{bidId}/actions/reject")
    public BidActionResponse rejectBid(
            @PathVariable Long bidId,
            @RequestBody(required = false) BidActionRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return bidService.rejectBid(
                principal.getAccountId(),
                principal.getRole(),
                bidId,
                request
        );
    }

    @PostMapping("/bids/{bidId}/actions/accept")
    public AcceptBidResponse acceptBid(
            @PathVariable Long bidId,
            @RequestBody(required = false) BidActionRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return bidService.acceptBid(
                principal.getAccountId(),
                principal.getRole(),
                bidId,
                request
        );
    }
}

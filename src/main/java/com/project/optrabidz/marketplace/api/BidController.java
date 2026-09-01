package com.project.optrabidz.marketplace.api;

import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.marketplace.application.BidService;
import com.project.optrabidz.marketplace.application.dto.request.BidActionRequest;
import com.project.optrabidz.marketplace.application.dto.request.SubmitBidRequest;
import com.project.optrabidz.marketplace.application.dto.response.AcceptBidResponse;
import com.project.optrabidz.marketplace.application.dto.response.BidActionResponse;
import com.project.optrabidz.marketplace.application.dto.response.BidResponse;
import com.project.optrabidz.marketplace.domain.model.BidState;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BidController {
    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping("/bids")
    public SuccessResponse<BidResponse> submitBid(@RequestBody @Valid SubmitBidRequest request,
                                                  @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                  HttpServletRequest httpRequest) {
        return ApiResponse.success(
                bidService.submitBid(
                        principal.getAccountId(),
                        principal.getRole(),
                        request
                ),
                httpRequest
        );
    }

    @GetMapping("/bids/{bidId}")
    public SuccessResponse<BidResponse> getBid(@PathVariable Long bidId,
                                               @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                               HttpServletRequest httpRequest) {
        return ApiResponse.success(
                bidService.getBidById(
                        principal.getAccountId(),
                        principal.getRole(),
                        bidId
                ),
                httpRequest
        );
    }

    @GetMapping("/bids")
    public SuccessResponse<PageResponse<BidResponse>> getBidsForListing(
            @RequestParam Long listingId,
            @RequestParam(required = false) BidState state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                bidService.getBidsForListing(
                        principal.getAccountId(),
                        principal.getRole(),
                        listingId,
                        state,
                        page,
                        size
                ),
                httpRequest
        );
    }

    @GetMapping("/investors/me/bids")
    public SuccessResponse<PageResponse<BidResponse>> getMyBids(
            @RequestParam(required = false) BidState state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                bidService.getMyBids(
                        principal.getAccountId(),
                        principal.getRole(),
                        state,
                        page,
                        size
                ),
                httpRequest
        );
    }

    @GetMapping("/investors/me/bids/by-listing/{listingId}")
    public SuccessResponse<BidResponse> getMyBidByListing(@PathVariable Long listingId,
                                                          @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                          HttpServletRequest httpRequest) {
        return ApiResponse.success(
                bidService.getMyBidByListing(
                        principal.getAccountId(),
                        principal.getRole(),
                        listingId
                ),
                httpRequest
        );
    }

    @GetMapping("/funding-listings/{listingId}/accepted-bid")
    public SuccessResponse<BidResponse> getAcceptedBid(@PathVariable Long listingId,
                                                       @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                       HttpServletRequest httpRequest) {
        return ApiResponse.success(
                bidService.getAcceptedBid(
                        principal.getAccountId(),
                        principal.getRole(),
                        listingId
                ),
                httpRequest
        );
    }

    @PostMapping("/bids/{bidId}/actions/withdraw")
    public SuccessResponse<BidActionResponse> withdrawBid(@PathVariable Long bidId,
                                                          @RequestBody(required = false) BidActionRequest request,
                                                          @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                          HttpServletRequest httpRequest) {
        return ApiResponse.success(
                bidService.withdrawBid(
                        principal.getAccountId(),
                        principal.getRole(),
                        bidId,
                        request
                ),
                httpRequest
        );
    }

    @PostMapping("/bids/{bidId}/actions/reject")
    public SuccessResponse<BidActionResponse> rejectBid(@PathVariable Long bidId,
                                                        @RequestBody(required = false) BidActionRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                        HttpServletRequest httpRequest) {
        return ApiResponse.success(
                bidService.rejectBid(
                        principal.getAccountId(),
                        principal.getRole(),
                        bidId,
                        request
                ),
                httpRequest
        );
    }

    @PostMapping("/bids/{bidId}/actions/accept")
    public SuccessResponse<AcceptBidResponse> acceptBid(@PathVariable Long bidId,
                                                        @RequestBody(required = false) BidActionRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                        HttpServletRequest httpRequest) {
        return ApiResponse.success(
                bidService.acceptBid(
                        principal.getAccountId(),
                        principal.getRole(),
                        bidId,
                        request
                ),
                httpRequest
        );
    }
}

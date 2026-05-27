package com.project.optrabidz.marketplace.api;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
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
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                bidService.submitBid(user.getAccountId(), user.getRole(), request),
                httpRequest
        );
    }

    @GetMapping("/bids/{bidId}")
    public SuccessResponse<BidResponse> getBid(@PathVariable Long bidId,
                                               @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                               HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                bidService.getBidById(user.getAccountId(), user.getRole(), bidId),
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
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                bidService.getBidsForListing(user.getAccountId(), user.getRole(), listingId, state, page, size),
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
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                bidService.getMyBids(user.getAccountId(), user.getRole(), state, page, size),
                httpRequest
        );
    }

    @GetMapping("/investors/me/bids/by-listing/{listingId}")
    public SuccessResponse<BidResponse> getMyBidByListing(@PathVariable Long listingId,
                                                          @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                          HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                bidService.getMyBidByListing(user.getAccountId(), user.getRole(), listingId),
                httpRequest
        );
    }

    @GetMapping("/funding-listings/{listingId}/accepted-bid")
    public SuccessResponse<BidResponse> getAcceptedBid(@PathVariable Long listingId,
                                                       @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                       HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                bidService.getAcceptedBid(user.getAccountId(), user.getRole(), listingId),
                httpRequest
        );
    }

    @PostMapping("/bids/{bidId}/actions/withdraw")
    public SuccessResponse<BidActionResponse> withdrawBid(@PathVariable Long bidId,
                                                          @RequestBody(required = false) BidActionRequest request,
                                                          @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                          HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                bidService.withdrawBid(user.getAccountId(), user.getRole(), bidId, request),
                httpRequest
        );
    }

    @PostMapping("/bids/{bidId}/actions/reject")
    public SuccessResponse<BidActionResponse> rejectBid(@PathVariable Long bidId,
                                                        @RequestBody(required = false) BidActionRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                        HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                bidService.rejectBid(user.getAccountId(), user.getRole(), bidId, request),
                httpRequest
        );
    }

    @PostMapping("/bids/{bidId}/actions/accept")
    public SuccessResponse<AcceptBidResponse> acceptBid(@PathVariable Long bidId,
                                                        @RequestBody(required = false) BidActionRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                        HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                bidService.acceptBid(user.getAccountId(), user.getRole(), bidId, request),
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

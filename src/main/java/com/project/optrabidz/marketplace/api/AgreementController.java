package com.project.optrabidz.marketplace.api;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.marketplace.application.AgreementService;
import com.project.optrabidz.marketplace.application.dto.response.AgreementResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AgreementController {
    private final AgreementService agreementService;

    public AgreementController(AgreementService agreementService) {
        this.agreementService = agreementService;
    }

    @GetMapping("/agreements/{agreementId}")
    public SuccessResponse<AgreementResponse> getAgreement(@PathVariable Long agreementId,
                                                           @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                           HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                agreementService.getAgreementById(user.getAccountId(), user.getRole(), agreementId),
                httpRequest
        );
    }

    @GetMapping("/startups/me/agreements")
    public SuccessResponse<PageResponse<AgreementResponse>> getMyStartupAgreements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                agreementService.getMyStartupAgreements(user.getAccountId(), user.getRole(), page, size),
                httpRequest
        );
    }

    @GetMapping("/investors/me/agreements")
    public SuccessResponse<PageResponse<AgreementResponse>> getMyInvestorAgreements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthenticatedUserPrincipal user = requirePrincipal(principal);
        return ApiResponse.success(
                agreementService.getMyInvestorAgreements(user.getAccountId(), user.getRole(), page, size),
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

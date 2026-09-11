package com.project.optrabidz.marketplace.api;

import com.project.optrabidz.common.application.pagination.PageResponse;
import com.project.optrabidz.marketplace.application.AgreementService;
import com.project.optrabidz.marketplace.application.dto.response.AgreementResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
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
    public AgreementResponse getAgreement(
            @PathVariable Long agreementId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return agreementService.getAgreementById(
                principal.getAccountId(),
                principal.getRole(),
                agreementId
        );
    }

    @GetMapping("/startups/me/agreements")
    public PageResponse<AgreementResponse> getMyStartupAgreements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return agreementService.getMyStartupAgreements(
                principal.getAccountId(),
                principal.getRole(),
                page,
                size
        );
    }

    @GetMapping("/investors/me/agreements")
    public PageResponse<AgreementResponse> getMyInvestorAgreements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return agreementService.getMyInvestorAgreements(
                principal.getAccountId(),
                principal.getRole(),
                page,
                size
        );
    }
}

package com.project.optrabidz.participation.api;

import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.MessageData;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.participation.application.InvestorService;
import com.project.optrabidz.participation.application.dto.request.CreateInvestorRequest;
import com.project.optrabidz.participation.application.dto.response.InvestorResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/investors")
public class InvestorController {
    private final InvestorService investorService;

    public InvestorController(InvestorService investorService) {
        this.investorService = investorService;
    }

    @PostMapping
    public SuccessResponse<MessageData> createInvestor(@RequestBody @Valid CreateInvestorRequest request,
                                                       @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                       AuthenticatedUserPrincipal principal,
                                                       HttpServletRequest httpRequest) {
        return ApiResponse.success(
                investorService.createInvestor(principal.getAccountId(), principal.getRole(), request),
                httpRequest
        );
    }

    @GetMapping("/me")
    public SuccessResponse<InvestorResponse> getMyInvestor(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                investorService.getMyInvestor(principal.getAccountId(), principal.getRole()),
                httpRequest
        );
    }

    @PatchMapping("/me")
    public SuccessResponse<MessageData> updateInvestor(@RequestBody @Valid CreateInvestorRequest request,
                                                       @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                       AuthenticatedUserPrincipal principal,
                                                       HttpServletRequest httpRequest) {
        return ApiResponse.success(
                investorService.updateInvestor(principal.getAccountId(), principal.getRole(), request),
                httpRequest
        );
    }
}

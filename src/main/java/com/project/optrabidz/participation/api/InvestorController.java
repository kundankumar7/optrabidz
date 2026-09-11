package com.project.optrabidz.participation.api;

import com.project.optrabidz.participation.application.InvestorService;
import com.project.optrabidz.participation.application.dto.request.CreateInvestorRequest;
import com.project.optrabidz.participation.application.dto.response.InvestorResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/investors")
public class InvestorController {
    private final InvestorService investorService;

    public InvestorController(InvestorService investorService) {
        this.investorService = investorService;
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Investor profile created",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = InvestorResponse.class)
            ),
            headers = @io.swagger.v3.oas.annotations.headers.Header(
                    name = "Location",
                    description = "URI of the created investor profile",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "string", format = "uri")
            )
    )
    public ResponseEntity<InvestorResponse> createInvestor(
            @RequestBody @Valid CreateInvestorRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            AuthenticatedUserPrincipal principal) {
        InvestorResponse response = investorService.createInvestor(
                principal.getAccountId(), principal.getRole(), request);
        return ResponseEntity.created(URI.create("/api/v1/investors/me")).body(response);
    }

    @GetMapping("/me")
    public InvestorResponse getMyInvestor(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return investorService.getMyInvestor(principal.getAccountId(), principal.getRole());
    }

    @PatchMapping("/me")
    public InvestorResponse updateInvestor(@RequestBody @Valid CreateInvestorRequest request,
                                           @org.springframework.security.core.annotation.AuthenticationPrincipal
                                           AuthenticatedUserPrincipal principal) {
        return investorService.updateInvestor(principal.getAccountId(), principal.getRole(), request);
    }
}

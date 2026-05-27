package com.project.optrabidz.participation.api;

import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.MessageData;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.participation.application.StartupService;
import com.project.optrabidz.participation.application.dto.request.CreateStartupRequest;
import com.project.optrabidz.participation.application.dto.response.StartupResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/startups")
public class StartupController {
    private final StartupService startupService;

    public StartupController(StartupService startupService) {
        this.startupService = startupService;
    }

    @PostMapping
    public SuccessResponse<MessageData> createStartup(@RequestBody @Valid CreateStartupRequest request,
                                                      @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                      AuthenticatedUserPrincipal principal,
                                                      HttpServletRequest httpRequest) {
        return ApiResponse.success(
                startupService.createStartup(principal.getAccountId(), principal.getRole(), request),
                httpRequest
        );
    }

    @GetMapping("/me")
    public SuccessResponse<StartupResponse> getMyStartup(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                startupService.getMyStartup(principal.getAccountId(), principal.getRole()),
                httpRequest
        );
    }

    @PatchMapping("/me")
    public SuccessResponse<MessageData> updateStartup(@RequestBody @Valid CreateStartupRequest request,
                                                      @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                      AuthenticatedUserPrincipal principal,
                                                      HttpServletRequest httpRequest) {
        return ApiResponse.success(
                startupService.updateStartup(principal.getAccountId(), principal.getRole(), request),
                httpRequest
        );
    }
}

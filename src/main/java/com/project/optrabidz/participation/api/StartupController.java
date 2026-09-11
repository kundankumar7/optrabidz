package com.project.optrabidz.participation.api;

import com.project.optrabidz.participation.application.StartupService;
import com.project.optrabidz.participation.application.dto.request.CreateStartupRequest;
import com.project.optrabidz.participation.application.dto.response.StartupResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/startups")
public class StartupController {
    private final StartupService startupService;

    public StartupController(StartupService startupService) {
        this.startupService = startupService;
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Startup profile created",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = StartupResponse.class)
            ),
            headers = @io.swagger.v3.oas.annotations.headers.Header(
                    name = "Location",
                    description = "URI of the created startup profile",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "string", format = "uri")
            )
    )
    public ResponseEntity<StartupResponse> createStartup(
            @RequestBody @Valid CreateStartupRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            AuthenticatedUserPrincipal principal) {
        StartupResponse response = startupService.createStartup(
                principal.getAccountId(), principal.getRole(), request);
        return ResponseEntity.created(URI.create("/api/v1/startups/me")).body(response);
    }

    @GetMapping("/me")
    public StartupResponse getMyStartup(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return startupService.getMyStartup(principal.getAccountId(), principal.getRole());
    }

    @PatchMapping("/me")
    public StartupResponse updateStartup(@RequestBody @Valid CreateStartupRequest request,
                                         @org.springframework.security.core.annotation.AuthenticationPrincipal
                                         AuthenticatedUserPrincipal principal) {
        return startupService.updateStartup(principal.getAccountId(), principal.getRole(), request);
    }
}

package com.project.optrabidz.security.api;

import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import com.project.optrabidz.security.application.AuthenticationService;
import com.project.optrabidz.security.application.dto.request.ChangePasswordRequest;
import com.project.optrabidz.security.application.dto.request.LoginRequest;
import com.project.optrabidz.security.application.dto.request.SignupRequest;
import com.project.optrabidz.security.application.dto.response.LoginResponse;
import com.project.optrabidz.security.application.dto.response.SignupResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Account registered",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = SignupResponse.class)
            )
    )
    public ResponseEntity<SignupResponse> register(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authenticationService.register(request));
    }

    @PostMapping("/login")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Authenticated account",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = LoginResponse.class)
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
                    responseCode = "500",
                    ref = "#/components/responses/InternalServerProblem"
            )
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest) {
        return authenticationService.login(request, httpRequest);
    }

    @PostMapping("/logout")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "Session ended"
    )
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        authenticationService.logout(httpRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "Password changed"
    )
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(principal, request);
        return ResponseEntity.noContent().build();
    }
}

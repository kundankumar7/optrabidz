package com.project.optrabidz.security.api;

import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import com.project.optrabidz.security.application.AuthenticationService;
import com.project.optrabidz.security.application.dto.request.ChangePasswordRequest;
import com.project.optrabidz.security.application.dto.request.LoginRequest;
import com.project.optrabidz.security.application.dto.request.SignupRequest;
import com.project.optrabidz.security.application.dto.response.LoginResponse;
import com.project.optrabidz.security.application.dto.response.MessageResponse;
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
    public ResponseEntity<SuccessResponse<SignupResponse>> register(@Valid @RequestBody SignupRequest request,
                                                                    HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authenticationService.register(request), httpRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                                HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(authenticationService.login(request, httpRequest), httpRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<MessageResponse>> logout(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(authenticationService.logout(httpRequest), httpRequest));
    }

    @PostMapping("/change-password")
    public ResponseEntity<SuccessResponse<MessageResponse>> changePassword(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                authenticationService.changePassword(principal, request),
                httpRequest
        ));
    }
}

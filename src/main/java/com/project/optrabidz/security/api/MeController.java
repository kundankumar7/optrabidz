package com.project.optrabidz.security.api;

import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.MessageData;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import com.project.optrabidz.security.application.MeService;
import com.project.optrabidz.security.application.dto.request.ChangePasswordRequest;
import com.project.optrabidz.security.application.dto.response.MeResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    public SuccessResponse<MeResponse> getCurrentUser(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                      HttpServletRequest httpRequest) {
        return ApiResponse.success(meService.getCurrentUser(principal), httpRequest);
    }
}

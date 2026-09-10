package com.project.optrabidz.security.api;

import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import com.project.optrabidz.security.application.MeService;
import com.project.optrabidz.security.application.dto.response.MeResponse;
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
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/UnauthorizedProblem"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    ref = "#/components/responses/InternalServerProblem"
            )
    })
    public MeResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return meService.getCurrentUser(principal);
    }
}

package com.project.optrabidz.classification.api;

import com.project.optrabidz.classification.application.command.AddInvestorPreferenceCommand;
import com.project.optrabidz.classification.application.command.ClassificationEntryCommand;
import com.project.optrabidz.classification.application.command.RemoveInvestorPreferenceCommand;
import com.project.optrabidz.classification.application.command.ReplaceInvestorPreferencesCommand;
import com.project.optrabidz.classification.application.dto.request.AddInvestorPreferenceRequest;
import com.project.optrabidz.classification.application.dto.request.ReplaceInvestorPreferencesRequest;
import com.project.optrabidz.classification.application.dto.response.InvestorPreferenceResponse;
import com.project.optrabidz.classification.application.port.in.InvestorPreferenceCommandPort;
import com.project.optrabidz.classification.application.port.in.InvestorPreferenceQueryPort;
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.MessageData;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/investor-preferences")
public class InvestorPreferenceController {
    private final InvestorPreferenceCommandPort commandPort;
    private final InvestorPreferenceQueryPort queryPort;

    public InvestorPreferenceController(InvestorPreferenceCommandPort commandPort,
                                        InvestorPreferenceQueryPort queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }

    @PostMapping
    public SuccessResponse<MessageData> addMyPreference(@RequestBody @Valid AddInvestorPreferenceRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                        HttpServletRequest httpRequest) {
        return ApiResponse.success(
                commandPort.addPreference(new AddInvestorPreferenceCommand(
                        principal.getAccountId(),
                        request.preferenceType(),
                        request.preferenceValue()
                )),
                httpRequest
        );
    }

    @PutMapping("/me")
    public SuccessResponse<MessageData> replaceMyPreferences(
            @RequestBody @Valid ReplaceInvestorPreferencesRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                commandPort.replacePreferences(new ReplaceInvestorPreferencesCommand(
                        principal.getAccountId(),
                        request.preferences().stream()
                                .map(entry -> new ClassificationEntryCommand(
                                        entry.preferenceType(),
                                        entry.preferenceValue()
                                ))
                                .toList()
                )),
                httpRequest
        );
    }

    @DeleteMapping("/me")
    public SuccessResponse<MessageData> removeMyPreference(@RequestParam String preferenceType,
                                                           @RequestParam String preferenceValue,
                                                           @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                           HttpServletRequest httpRequest) {
        return ApiResponse.success(
                commandPort.removePreference(new RemoveInvestorPreferenceCommand(
                        principal.getAccountId(),
                        preferenceType,
                        preferenceValue
                )),
                httpRequest
        );
    }

    @GetMapping("/me")
    public SuccessResponse<InvestorPreferenceResponse> getMyPreferences(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(queryPort.getMyPreferences(principal.getAccountId()), httpRequest);
    }
}

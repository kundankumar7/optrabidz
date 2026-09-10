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
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> addMyPreference(
            @RequestBody @Valid AddInvestorPreferenceRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        commandPort.addPreference(new AddInvestorPreferenceCommand(
                principal.getAccountId(),
                request.preferenceType(),
                request.preferenceValue()
        ));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me")
    public ResponseEntity<Void> replaceMyPreferences(
            @RequestBody @Valid ReplaceInvestorPreferencesRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        commandPort.replacePreferences(new ReplaceInvestorPreferencesCommand(
                principal.getAccountId(),
                request.preferences().stream()
                        .map(entry -> new ClassificationEntryCommand(
                                entry.preferenceType(),
                                entry.preferenceValue()
                        ))
                        .toList()
        ));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> removeMyPreference(
            @RequestParam String preferenceType,
            @RequestParam String preferenceValue,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        commandPort.removePreference(new RemoveInvestorPreferenceCommand(
                principal.getAccountId(),
                preferenceType,
                preferenceValue
        ));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public InvestorPreferenceResponse getMyPreferences(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return queryPort.getMyPreferences(principal.getAccountId());
    }
}

package com.project.optrabidz.classification.api;

import com.project.optrabidz.classification.application.command.AddStartupClassificationCommand;
import com.project.optrabidz.classification.application.command.ClassificationEntryCommand;
import com.project.optrabidz.classification.application.command.RemoveStartupClassificationCommand;
import com.project.optrabidz.classification.application.command.ReplaceStartupClassificationsCommand;
import com.project.optrabidz.classification.application.dto.request.AddStartupClassificationRequest;
import com.project.optrabidz.classification.application.dto.request.ReplaceStartupClassificationsRequest;
import com.project.optrabidz.classification.application.dto.response.StartupClassificationResponse;
import com.project.optrabidz.classification.application.port.in.StartupClassificationCommandPort;
import com.project.optrabidz.classification.application.port.in.StartupClassificationQueryPort;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/startup-classifications")
public class StartupClassificationController {
    private final StartupClassificationCommandPort commandPort;
    private final StartupClassificationQueryPort queryPort;

    public StartupClassificationController(StartupClassificationCommandPort commandPort,
                                           StartupClassificationQueryPort queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "Classification added"
    )
    public ResponseEntity<Void> addMyClassification(
            @RequestBody @Valid AddStartupClassificationRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        commandPort.addClassification(new AddStartupClassificationCommand(
                principal.getAccountId(),
                request.classificationType(),
                request.classificationValue()
        ));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "Classifications replaced"
    )
    public ResponseEntity<Void> replaceMyClassifications(
            @RequestBody @Valid ReplaceStartupClassificationsRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        commandPort.replaceClassifications(new ReplaceStartupClassificationsCommand(
                principal.getAccountId(),
                request.classifications().stream()
                        .map(entry -> new ClassificationEntryCommand(
                                entry.classificationType(),
                                entry.classificationValue()
                        ))
                        .toList()
        ));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "Classification removed"
    )
    public ResponseEntity<Void> removeMyClassification(
            @RequestParam String classificationType,
            @RequestParam String classificationValue,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        commandPort.removeClassification(new RemoveStartupClassificationCommand(
                principal.getAccountId(),
                classificationType,
                classificationValue
        ));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public StartupClassificationResponse getMyClassifications(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return queryPort.getMyClassifications(principal.getAccountId());
    }
}

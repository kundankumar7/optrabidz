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
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.MessageData;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.security.application.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    public SuccessResponse<MessageData> addMyClassification(@RequestBody @Valid AddStartupClassificationRequest request,
                                                            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                commandPort.addClassification(new AddStartupClassificationCommand(
                        principal.getAccountId(),
                        request.classificationType(),
                        request.classificationValue()
                )),
                httpRequest
        );
    }

    @PutMapping("/me")
    public SuccessResponse<MessageData> replaceMyClassifications(
            @RequestBody @Valid ReplaceStartupClassificationsRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(
                commandPort.replaceClassifications(new ReplaceStartupClassificationsCommand(
                        principal.getAccountId(),
                        request.classifications().stream()
                                .map(entry -> new ClassificationEntryCommand(
                                        entry.classificationType(),
                                        entry.classificationValue()
                                ))
                                .toList()
                )),
                httpRequest
        );
    }

    @DeleteMapping("/me")
    public SuccessResponse<MessageData> removeMyClassification(@RequestParam String classificationType,
                                                               @RequestParam String classificationValue,
                                                               @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                                               HttpServletRequest httpRequest) {
        return ApiResponse.success(
                commandPort.removeClassification(new RemoveStartupClassificationCommand(
                        principal.getAccountId(),
                        classificationType,
                        classificationValue
                )),
                httpRequest
        );
    }

    @GetMapping("/me")
    public SuccessResponse<StartupClassificationResponse> getMyClassifications(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(queryPort.getMyClassifications(principal.getAccountId()), httpRequest);
    }
}

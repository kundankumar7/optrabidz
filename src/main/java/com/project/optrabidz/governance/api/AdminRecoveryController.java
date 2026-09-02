package com.project.optrabidz.governance.api;

import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.governance.application.admin.AdminAuthorityTransferService;
import com.project.optrabidz.governance.application.admin.AdminRecoveryProperties;
import com.project.optrabidz.governance.application.admin.AdminTransferResponse;
import com.project.optrabidz.governance.application.admin.TransferAdminAuthorityRequest;
import com.project.optrabidz.governance.application.admin.exception.AdminRecoveryAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/admin/recovery")
@ConditionalOnProperty(prefix = "optrabidz.admin.recovery", name = "enabled", havingValue = "true")
public class AdminRecoveryController {
    private static final String RECOVERY_TOKEN_HEADER = "X-ADMIN-RECOVERY-TOKEN";

    private final AdminAuthorityTransferService transferService;
    private final AdminRecoveryProperties properties;

    public AdminRecoveryController(AdminAuthorityTransferService transferService,
                                   AdminRecoveryProperties properties) {
        this.transferService = transferService;
        this.properties = properties;
    }

    @PostMapping("/transfer")
    public SuccessResponse<AdminTransferResponse> transferAdminAuthority(
            @RequestHeader(name = RECOVERY_TOKEN_HEADER, required = false) String recoveryToken,
            @Valid @RequestBody TransferAdminAuthorityRequest request,
            HttpServletRequest httpRequest) {
        assertRecoveryAccess(recoveryToken);

        Long newAdminAccountId = transferService.transferAuthority(
                request.toCommand(),
                properties.isEnabled()
        );

        return ApiResponse.success(
                new AdminTransferResponse(
                        newAdminAccountId,
                        "Admin authority transferred successfully"
                ),
                httpRequest
        );
    }

    private void assertRecoveryAccess(String recoveryToken) {
        if (!properties.isEnabled()) {
            throw AdminRecoveryAccessDeniedException.recoveryModeDisabled();
        }

        if (properties.getToken() == null || properties.getToken().isBlank()) {
            throw AdminRecoveryAccessDeniedException.tokenNotConfigured();
        }

        if (recoveryToken == null || recoveryToken.isBlank()) {
            throw AdminRecoveryAccessDeniedException.tokenMissing();
        }

        if (!secureEquals(properties.getToken(), recoveryToken)) {
            throw AdminRecoveryAccessDeniedException.tokenRejected();
        }
    }

    private boolean secureEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}

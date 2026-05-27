package com.project.optrabidz.governance.api;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.common.api.response.ApiResponse;
import com.project.optrabidz.common.api.response.SuccessResponse;
import com.project.optrabidz.governance.application.admin.AdminAuthorityTransferService;
import com.project.optrabidz.governance.application.admin.AdminBootstrapProperties;
import com.project.optrabidz.governance.application.admin.AdminTransferResponse;
import com.project.optrabidz.governance.application.admin.TransferAdminAuthorityRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/admin/recovery")
public class AdminRecoveryController {
    private static final String RECOVERY_TOKEN_HEADER = "X-ADMIN-RECOVERY-TOKEN";

    private final AdminAuthorityTransferService transferService;
    private final AdminBootstrapProperties properties;

    public AdminRecoveryController(AdminAuthorityTransferService transferService,
                                   AdminBootstrapProperties properties) {
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
                properties.isRecoveryMode()
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
        if (!properties.isRecoveryMode()) {
            throw new ApiException(
                    ErrorCode.AUTHORIZATION_FAILED,
                    "Admin recovery mode is disabled"
            );
        }

        if (properties.getRecoveryToken() == null || properties.getRecoveryToken().isBlank()) {
            throw new ApiException(
                    ErrorCode.AUTHORIZATION_FAILED,
                    "Admin recovery token is not configured"
            );
        }

        if (recoveryToken == null || recoveryToken.isBlank()
                || !secureEquals(properties.getRecoveryToken(), recoveryToken)) {
            throw new ApiException(
                    ErrorCode.AUTHORIZATION_FAILED,
                    "Invalid admin recovery token"
            );
        }
    }

    private boolean secureEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}

package com.project.optrabidz.governance.application.admin;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.participation.application.port.AdminAuthorityQueryPort;
import org.springframework.stereotype.Component;

@Component
public class AdministrativeAuthorityGuard {
    private final AdminAuthorityQueryPort adminAuthorityQueryPort;

    public AdministrativeAuthorityGuard(AdminAuthorityQueryPort adminAuthorityQueryPort) {
        this.adminAuthorityQueryPort = adminAuthorityQueryPort;
    }

    public boolean canBootstrapFirstAdmin() {
        return !adminAuthorityQueryPort.activeAdminExists();
    }

    public void assertRecoveryTransferAllowed(boolean recoveryModeEnabled) {
        if (!recoveryModeEnabled) {
            throw new ApiException(
                    ErrorCode.AUTHORIZATION_FAILED,
                    "Admin authority transfer requires recovery mode"
            );
        }

        if (!adminAuthorityQueryPort.activeAdminExists()) {
            throw new ApiException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "No active admin exists to transfer"
            );
        }
    }

    public void assertActiveAdmin(Long accountId) {
        if (!adminAuthorityQueryPort.isActiveAdmin(accountId)) {
            throw new ApiException(
                    ErrorCode.AUTHORIZATION_FAILED,
                    "Only the active admin authority can perform this action"
            );
        }
    }
}

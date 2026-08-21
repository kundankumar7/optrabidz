package com.project.optrabidz.governance.application.admin;

import com.project.optrabidz.governance.application.admin.exception.AdminAuthorityUnavailableException;
import com.project.optrabidz.governance.application.admin.exception.AdminRecoveryAccessDeniedException;
import com.project.optrabidz.governance.application.common.GovernanceDecision;
import com.project.optrabidz.governance.application.common.GovernanceException;
import com.project.optrabidz.governance.application.common.GovernanceRuleCode;
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
            throw AdminRecoveryAccessDeniedException.recoveryModeDisabled();
        }

        if (!adminAuthorityQueryPort.activeAdminExists()) {
            throw new AdminAuthorityUnavailableException();
        }
    }

    public void assertActiveAdmin(Long accountId) {
        if (!adminAuthorityQueryPort.isActiveAdmin(accountId)) {
            throw new GovernanceException(GovernanceDecision.deny(
                    GovernanceRuleCode.ADMIN_AUTHORITY_REQUIRED,
                    "admin",
                    "Active admin authority is required"
            ));
        }
    }
}

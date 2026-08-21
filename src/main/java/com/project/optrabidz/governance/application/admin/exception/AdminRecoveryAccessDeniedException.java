package com.project.optrabidz.governance.application.admin.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.governance.application.error.GovernanceErrors;

public final class AdminRecoveryAccessDeniedException extends ApplicationException {

    private AdminRecoveryAccessDeniedException(
            String diagnosticCode,
            String diagnosticMessage
    ) {
        super(
                GovernanceErrors.ADMIN_RECOVERY_ACCESS_DENIED,
                diagnosticCode,
                diagnosticMessage
        );
    }

    public static AdminRecoveryAccessDeniedException recoveryModeDisabled() {
        return new AdminRecoveryAccessDeniedException(
                "GOVERNANCE.RECOVERY.MODE_DISABLED",
                "Admin recovery mode is disabled"
        );
    }

    public static AdminRecoveryAccessDeniedException tokenNotConfigured() {
        return new AdminRecoveryAccessDeniedException(
                "GOVERNANCE.RECOVERY.TOKEN_NOT_CONFIGURED",
                "Admin recovery token is not configured"
        );
    }

    public static AdminRecoveryAccessDeniedException tokenMissing() {
        return new AdminRecoveryAccessDeniedException(
                "GOVERNANCE.RECOVERY.TOKEN_MISSING",
                "Admin recovery token was not submitted"
        );
    }

    public static AdminRecoveryAccessDeniedException tokenRejected() {
        return new AdminRecoveryAccessDeniedException(
                "GOVERNANCE.RECOVERY.TOKEN_REJECTED",
                "Submitted admin recovery token was rejected"
        );
    }
}

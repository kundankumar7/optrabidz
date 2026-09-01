package com.project.optrabidz.governance.application.admin.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.governance.application.error.GovernanceErrors;

public final class AdminAuthorityUnavailableException extends ApplicationException {

    public AdminAuthorityUnavailableException() {
        super(
                GovernanceErrors.ADMIN_AUTHORITY_UNAVAILABLE,
                "GOVERNANCE.ADMIN_AUTHORITY.UNAVAILABLE",
                "No active admin authority exists to transfer"
        );
    }
}

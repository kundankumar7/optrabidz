package com.project.optrabidz.participation.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.participation.application.error.AdminErrors;

public final class ActiveAdminNotFoundException extends ApplicationException {

    public ActiveAdminNotFoundException() {
        super(
                AdminErrors.ACTIVE_ADMIN_NOT_FOUND,
                "PARTICIPATION.ADMIN.ACTIVE_NOT_FOUND",
                "No active administrator is available for revocation"
        );
    }
}

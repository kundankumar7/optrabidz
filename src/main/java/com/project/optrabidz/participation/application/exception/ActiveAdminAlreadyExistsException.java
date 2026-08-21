package com.project.optrabidz.participation.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.participation.application.error.AdminErrors;

public final class ActiveAdminAlreadyExistsException extends ApplicationException {

    public ActiveAdminAlreadyExistsException() {
        super(
                AdminErrors.ACTIVE_ADMIN_ALREADY_EXISTS,
                "PARTICIPATION.ADMIN.ACTIVE_ALREADY_EXISTS",
                "An active administrator prevents a new authority grant"
        );
    }
}

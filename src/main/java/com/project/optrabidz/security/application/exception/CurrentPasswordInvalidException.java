package com.project.optrabidz.security.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.security.application.error.SecurityErrors;

public final class CurrentPasswordInvalidException extends ApplicationException {

    public CurrentPasswordInvalidException(Long accountId) {
        super(
                SecurityErrors.CURRENT_PASSWORD_INVALID,
                "SECURITY.PASSWORD.CURRENT_INVALID",
                "Current password rejected for account " + accountId
        );
    }
}

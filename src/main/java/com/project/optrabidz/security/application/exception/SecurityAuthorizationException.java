package com.project.optrabidz.security.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.security.application.error.SecurityErrors;

public final class SecurityAuthorizationException extends ApplicationException {

    public SecurityAuthorizationException(Long accountId, String operation) {
        super(
                SecurityErrors.AUTHORIZATION_FAILED,
                "SECURITY.AUTHORIZATION.FAILED",
                "Account " + accountId + " is not authorized to " + operation
        );
    }
}

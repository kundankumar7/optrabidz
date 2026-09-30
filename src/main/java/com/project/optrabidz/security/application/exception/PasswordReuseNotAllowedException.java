package com.project.optrabidz.security.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.security.application.error.SecurityErrors;

public final class PasswordReuseNotAllowedException extends ApplicationException {

    public PasswordReuseNotAllowedException(Long accountId) {
        super(
                SecurityErrors.PASSWORD_REUSE_NOT_ALLOWED,
                "SECURITY.PASSWORD.REUSE_NOT_ALLOWED",
                "Password reuse rejected for account " + accountId
        );
    }
}

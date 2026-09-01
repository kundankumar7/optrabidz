package com.project.optrabidz.security.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.security.application.LoginFailureReason;
import com.project.optrabidz.security.application.error.SecurityErrors;

public final class InvalidCredentialsException extends ApplicationException {

    public InvalidCredentialsException(LoginFailureReason reason) {
        super(
                SecurityErrors.INVALID_CREDENTIALS,
                "SECURITY.LOGIN." + reason.name(),
                "Login rejected: " + reason.name()
        );
    }
}

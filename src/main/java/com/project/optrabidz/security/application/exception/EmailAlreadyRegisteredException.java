package com.project.optrabidz.security.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.security.application.error.SecurityErrors;

public final class EmailAlreadyRegisteredException extends ApplicationException {

    public EmailAlreadyRegisteredException(String email) {
        super(
                SecurityErrors.EMAIL_ALREADY_REGISTERED,
                "SECURITY.EMAIL.ALREADY_REGISTERED",
                "Email is already registered: " + email
        );
    }
}

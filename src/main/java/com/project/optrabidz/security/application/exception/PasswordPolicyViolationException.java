package com.project.optrabidz.security.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.security.application.error.SecurityErrors;

public final class PasswordPolicyViolationException extends ApplicationException {

    public PasswordPolicyViolationException() {
        super(
                SecurityErrors.PASSWORD_POLICY_VIOLATION,
                "SECURITY.PASSWORD.POLICY_VIOLATION",
                "Password rejected by security policy"
        );
    }
}

package com.project.optrabidz.security.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

public final class SecurityErrors {

    public static final ErrorDescriptor INVALID_CREDENTIALS = descriptor(
            "INVALID_CREDENTIALS",
            ErrorCategory.AUTHENTICATION,
            "Invalid email or password"
    );

    public static final ErrorDescriptor CURRENT_PASSWORD_INVALID = descriptor(
            "CURRENT_PASSWORD_INVALID",
            ErrorCategory.AUTHENTICATION,
            "Current password is incorrect"
    );

    public static final ErrorDescriptor EMAIL_ALREADY_REGISTERED = descriptor(
            "EMAIL_ALREADY_REGISTERED",
            ErrorCategory.CONFLICT,
            "Email is already registered"
    );

    public static final ErrorDescriptor CREDENTIAL_NOT_FOUND = descriptor(
            "CREDENTIAL_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested credential was not found"
    );

    public static final ErrorDescriptor PASSWORD_POLICY_VIOLATION = descriptor(
            "PASSWORD_POLICY_VIOLATION",
            ErrorCategory.VALIDATION,
            "Password must contain at least one letter and one digit"
    );

    public static final ErrorDescriptor SELF_REGISTRATION_NOT_ALLOWED = descriptor(
            "SELF_REGISTRATION_NOT_ALLOWED",
            ErrorCategory.BUSINESS_RULE,
            "Only startup or investor accounts can self-register"
    );

    public static final ErrorDescriptor AUTHORIZATION_FAILED = descriptor(
            "AUTHORIZATION_FAILED",
            ErrorCategory.AUTHORIZATION,
            "You are not authorized to perform this action"
    );

    private SecurityErrors() {
    }

    private static ErrorDescriptor descriptor(
            String code,
            ErrorCategory category,
            String publicMessage
    ) {
        return new ErrorDescriptor(code, category, publicMessage);
    }
}

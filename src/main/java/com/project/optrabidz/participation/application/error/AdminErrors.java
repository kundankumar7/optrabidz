package com.project.optrabidz.participation.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

public final class AdminErrors {

    public static final ErrorDescriptor ACTIVE_ADMIN_ALREADY_EXISTS = descriptor(
            "ACTIVE_ADMIN_ALREADY_EXISTS",
            ErrorCategory.CONFLICT,
            "An active administrator already exists"
    );

    public static final ErrorDescriptor ADMIN_AUTHORITY_ALREADY_GRANTED = descriptor(
            "ADMIN_AUTHORITY_ALREADY_GRANTED",
            ErrorCategory.CONFLICT,
            "Administrator authority was previously granted to this account"
    );

    public static final ErrorDescriptor ACTIVE_ADMIN_NOT_FOUND = descriptor(
            "ACTIVE_ADMIN_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "No active administrator was found"
    );

    private AdminErrors() {
    }

    private static ErrorDescriptor descriptor(
            String code,
            ErrorCategory category,
            String publicMessage
    ) {
        return new ErrorDescriptor(code, category, publicMessage);
    }
}

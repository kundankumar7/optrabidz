package com.project.optrabidz.identity.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

import java.util.List;

public final class IdentityErrors {

    public static final ErrorDescriptor ACCOUNT_NOT_FOUND = new ErrorDescriptor(
            "ACCOUNT_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested account was not found"
    );

    public static final ErrorDescriptor ACCOUNT_STATE_CONFLICT = new ErrorDescriptor(
            "ACCOUNT_STATE_CONFLICT",
            ErrorCategory.CONFLICT,
            "The account state does not allow this operation"
    );

    public static final ErrorDescriptor PROFILE_STATE_CONFLICT = new ErrorDescriptor(
            "PROFILE_STATE_CONFLICT",
            ErrorCategory.CONFLICT,
            "The profile state does not allow this operation"
    );

    public static List<ErrorDescriptor> descriptors() {
        return List.of(
                ACCOUNT_NOT_FOUND,
                ACCOUNT_STATE_CONFLICT,
                PROFILE_STATE_CONFLICT
        );
    }

    private IdentityErrors() {
    }
}

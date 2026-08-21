package com.project.optrabidz.participation.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

public final class ParticipationErrors {

    public static final ErrorDescriptor AUTHORIZATION_FAILED = new ErrorDescriptor(
            "AUTHORIZATION_FAILED",
            ErrorCategory.AUTHORIZATION,
            "You are not authorized to perform this action"
    );

    private ParticipationErrors() {
    }
}

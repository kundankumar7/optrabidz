package com.project.optrabidz.participation.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

import java.util.List;

public final class ParticipationErrors {

    public static final ErrorDescriptor AUTHORIZATION_FAILED = new ErrorDescriptor(
            "AUTHORIZATION_FAILED",
            ErrorCategory.AUTHORIZATION,
            "You are not authorized to perform this action"
    );

    public static List<ErrorDescriptor> descriptors() {
        return List.of(AUTHORIZATION_FAILED);
    }

    private ParticipationErrors() {
    }
}

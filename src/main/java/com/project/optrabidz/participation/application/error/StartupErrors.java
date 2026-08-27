package com.project.optrabidz.participation.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

import java.util.List;

public final class StartupErrors {

    public static final ErrorDescriptor STARTUP_ALREADY_EXISTS = descriptor(
            "STARTUP_ALREADY_EXISTS",
            ErrorCategory.CONFLICT,
            "A startup profile already exists"
    );

    public static final ErrorDescriptor STARTUP_NOT_FOUND = descriptor(
            "STARTUP_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested startup profile was not found"
    );

    public static List<ErrorDescriptor> descriptors() {
        return List.of(
                STARTUP_ALREADY_EXISTS,
                STARTUP_NOT_FOUND
        );
    }

    private StartupErrors() {
    }

    private static ErrorDescriptor descriptor(
            String code,
            ErrorCategory category,
            String publicMessage
    ) {
        return new ErrorDescriptor(code, category, publicMessage);
    }
}

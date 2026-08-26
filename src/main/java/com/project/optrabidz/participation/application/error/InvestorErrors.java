package com.project.optrabidz.participation.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

import java.util.List;

public final class InvestorErrors {

    public static final ErrorDescriptor INVESTOR_ALREADY_EXISTS = descriptor(
            "INVESTOR_ALREADY_EXISTS",
            ErrorCategory.CONFLICT,
            "An investor profile already exists"
    );

    public static final ErrorDescriptor INVESTOR_NOT_FOUND = descriptor(
            "INVESTOR_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested investor profile was not found"
    );

    public static List<ErrorDescriptor> descriptors() {
        return List.of(
                INVESTOR_ALREADY_EXISTS,
                INVESTOR_NOT_FOUND
        );
    }

    private InvestorErrors() {
    }

    private static ErrorDescriptor descriptor(
            String code,
            ErrorCategory category,
            String publicMessage
    ) {
        return new ErrorDescriptor(code, category, publicMessage);
    }
}

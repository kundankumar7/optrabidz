package com.project.optrabidz.classification.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

import java.util.List;

public final class ClassificationErrors {

    public static final ErrorDescriptor STARTUP_CLASSIFICATION_PROFILE_REQUIRED =
            new ErrorDescriptor(
                    "STARTUP_CLASSIFICATION_PROFILE_REQUIRED",
                    ErrorCategory.BUSINESS_RULE,
                    "Create a startup profile before managing classifications"
            );

    public static final ErrorDescriptor STARTUP_CLASSIFICATION_ALREADY_EXISTS =
            new ErrorDescriptor(
                    "STARTUP_CLASSIFICATION_ALREADY_EXISTS",
                    ErrorCategory.CONFLICT,
                    "The startup classification already exists"
            );

    public static final ErrorDescriptor STARTUP_CLASSIFICATION_NOT_FOUND =
            new ErrorDescriptor(
                    "STARTUP_CLASSIFICATION_NOT_FOUND",
                    ErrorCategory.NOT_FOUND,
                    "The requested startup classification was not found"
            );

    public static final ErrorDescriptor STARTUP_CLASSIFICATION_RULE_VIOLATION =
            new ErrorDescriptor(
                    "STARTUP_CLASSIFICATION_RULE_VIOLATION",
                    ErrorCategory.BUSINESS_RULE,
                    "The startup classification does not satisfy classification rules"
            );

    public static final ErrorDescriptor INVESTOR_PREFERENCE_PROFILE_REQUIRED =
            new ErrorDescriptor(
                    "INVESTOR_PREFERENCE_PROFILE_REQUIRED",
                    ErrorCategory.BUSINESS_RULE,
                    "Create an investor profile before managing preferences"
            );

    public static final ErrorDescriptor INVESTOR_PREFERENCE_ALREADY_EXISTS =
            new ErrorDescriptor(
                    "INVESTOR_PREFERENCE_ALREADY_EXISTS",
                    ErrorCategory.CONFLICT,
                    "The investor preference already exists"
            );

    public static final ErrorDescriptor INVESTOR_PREFERENCE_NOT_FOUND =
            new ErrorDescriptor(
                    "INVESTOR_PREFERENCE_NOT_FOUND",
                    ErrorCategory.NOT_FOUND,
                    "The requested investor preference was not found"
            );

    public static final ErrorDescriptor INVESTOR_PREFERENCE_RULE_VIOLATION =
            new ErrorDescriptor(
                    "INVESTOR_PREFERENCE_RULE_VIOLATION",
                    ErrorCategory.BUSINESS_RULE,
                    "The investor preference does not satisfy preference rules"
            );

    public static List<ErrorDescriptor> descriptors() {
        return List.of(
                STARTUP_CLASSIFICATION_PROFILE_REQUIRED,
                STARTUP_CLASSIFICATION_ALREADY_EXISTS,
                STARTUP_CLASSIFICATION_NOT_FOUND,
                STARTUP_CLASSIFICATION_RULE_VIOLATION,
                INVESTOR_PREFERENCE_PROFILE_REQUIRED,
                INVESTOR_PREFERENCE_ALREADY_EXISTS,
                INVESTOR_PREFERENCE_NOT_FOUND,
                INVESTOR_PREFERENCE_RULE_VIOLATION
        );
    }

    private ClassificationErrors() {
    }
}

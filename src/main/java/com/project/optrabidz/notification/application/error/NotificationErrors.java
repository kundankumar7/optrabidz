package com.project.optrabidz.notification.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

import java.util.List;

public final class NotificationErrors {
    public static final ErrorDescriptor NOTIFICATION_NOT_FOUND =
            new ErrorDescriptor(
                    "NOTIFICATION_NOT_FOUND",
                    ErrorCategory.NOT_FOUND,
                    "The requested notification was not found"
            );

    public static final ErrorDescriptor NOTIFICATION_SUBSCRIPTION_NOT_FOUND =
            new ErrorDescriptor(
                    "NOTIFICATION_SUBSCRIPTION_NOT_FOUND",
                    ErrorCategory.NOT_FOUND,
                    "The requested notification subscription was not found"
            );

    public static List<ErrorDescriptor> descriptors() {
        return List.of(
                NOTIFICATION_NOT_FOUND,
                NOTIFICATION_SUBSCRIPTION_NOT_FOUND
        );
    }

    private NotificationErrors() {
    }
}

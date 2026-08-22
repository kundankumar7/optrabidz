package com.project.optrabidz.notification.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.notification.application.error.NotificationErrors;

public final class NotificationSubscriptionNotFoundException extends ApplicationException {
    public NotificationSubscriptionNotFoundException(String diagnosticMessage) {
        super(
                NotificationErrors.NOTIFICATION_SUBSCRIPTION_NOT_FOUND,
                "NOTIFICATION.SUBSCRIPTION.NOT_FOUND",
                diagnosticMessage
        );
    }
}

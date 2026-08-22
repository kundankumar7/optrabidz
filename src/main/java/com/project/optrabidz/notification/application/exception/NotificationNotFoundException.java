package com.project.optrabidz.notification.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.notification.application.error.NotificationErrors;

public final class NotificationNotFoundException extends ApplicationException {
    public NotificationNotFoundException(String diagnosticMessage) {
        super(
                NotificationErrors.NOTIFICATION_NOT_FOUND,
                "NOTIFICATION.RECIPIENT.NOT_FOUND",
                diagnosticMessage
        );
    }
}

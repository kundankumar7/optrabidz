package com.project.optrabidz.notification.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class NotificationNotFoundException extends ApiException {
    public NotificationNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND, "Notification was not found");
    }
}

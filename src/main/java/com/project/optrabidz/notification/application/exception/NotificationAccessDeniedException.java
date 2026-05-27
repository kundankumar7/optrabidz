package com.project.optrabidz.notification.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class NotificationAccessDeniedException extends ApiException {
    public NotificationAccessDeniedException() {
        super(ErrorCode.AUTHORIZATION_FAILED, "You are not authorized to access this notification");
    }
}

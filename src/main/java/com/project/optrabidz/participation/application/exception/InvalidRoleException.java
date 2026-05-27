package com.project.optrabidz.participation.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class InvalidRoleException extends ApiException {
    public InvalidRoleException(String message) {
        super(ErrorCode.AUTHORIZATION_FAILED, message);
    }
}

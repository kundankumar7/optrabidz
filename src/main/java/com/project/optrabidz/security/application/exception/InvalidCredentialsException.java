package com.project.optrabidz.security.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException(String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }
}

package com.project.optrabidz.security.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class EmailAlreadyExistsException extends ApiException {
    public EmailAlreadyExistsException(String message) {
        super(ErrorCode.DUPLICATE_OPERATION, message);
    }
}

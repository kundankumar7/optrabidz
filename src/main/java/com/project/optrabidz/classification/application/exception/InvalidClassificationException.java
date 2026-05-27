package com.project.optrabidz.classification.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class InvalidClassificationException extends ApiException {
    public InvalidClassificationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}

package com.project.optrabidz.classification.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class ClassificationAlreadyExistsException extends ApiException {
    public ClassificationAlreadyExistsException(String message) {
        super(ErrorCode.DUPLICATE_OPERATION, message);
    }
}

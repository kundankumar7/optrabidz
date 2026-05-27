package com.project.optrabidz.participation.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class ParticipationAlreadyExistsException extends ApiException {
    public ParticipationAlreadyExistsException(String message) {
        super(ErrorCode.DUPLICATE_OPERATION, message);
    }
}

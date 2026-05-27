package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class InvalidBidStateException extends ApiException {
    public InvalidBidStateException(String message) {
        super(ErrorCode.INVALID_STATE_TRANSITION, message);
    }
}

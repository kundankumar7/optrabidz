package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class InvalidListingStateException extends ApiException {
    public InvalidListingStateException(String message) {
        super(ErrorCode.INVALID_STATE_TRANSITION, message);
    }
}

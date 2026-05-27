package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class InvalidRepaymentStateException extends ApiException {
    public InvalidRepaymentStateException(String message) {
        super(ErrorCode.INVALID_STATE_TRANSITION, message);
    }
}

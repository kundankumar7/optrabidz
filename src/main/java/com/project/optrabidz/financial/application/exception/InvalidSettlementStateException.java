package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class InvalidSettlementStateException extends ApiException {
    public InvalidSettlementStateException(String message) {
        super(ErrorCode.INVALID_STATE_TRANSITION, message);
    }
}

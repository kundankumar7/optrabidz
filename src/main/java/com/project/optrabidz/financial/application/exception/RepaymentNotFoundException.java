package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class RepaymentNotFoundException extends ApiException {
    public RepaymentNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}

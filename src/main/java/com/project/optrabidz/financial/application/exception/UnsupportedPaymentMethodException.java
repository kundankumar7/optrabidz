package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class UnsupportedPaymentMethodException extends ApiException {
    public UnsupportedPaymentMethodException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}

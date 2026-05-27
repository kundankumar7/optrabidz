package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class PaymentAlreadyConfirmedException extends ApiException {
    public PaymentAlreadyConfirmedException(String message) {
        super(ErrorCode.PAYMENT_ALREADY_CONFIRMED, message);
    }
}

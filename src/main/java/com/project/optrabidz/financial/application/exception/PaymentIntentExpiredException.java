package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class PaymentIntentExpiredException extends ApiException {
    public PaymentIntentExpiredException(String message) {
        super(ErrorCode.PAYMENT_INTENT_EXPIRED, message);
    }
}

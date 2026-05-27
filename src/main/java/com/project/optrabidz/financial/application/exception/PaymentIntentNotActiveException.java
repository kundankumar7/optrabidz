package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class PaymentIntentNotActiveException extends ApiException {
    public PaymentIntentNotActiveException(String message) {
        super(ErrorCode.PAYMENT_INTENT_NOT_ACTIVE, message);
    }
}

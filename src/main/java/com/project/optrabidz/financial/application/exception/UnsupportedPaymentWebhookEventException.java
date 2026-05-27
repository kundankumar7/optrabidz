package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class UnsupportedPaymentWebhookEventException extends ApiException {
    public UnsupportedPaymentWebhookEventException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}

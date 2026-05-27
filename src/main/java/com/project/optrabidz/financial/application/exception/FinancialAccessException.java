package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class FinancialAccessException extends ApiException {
    public FinancialAccessException(String message) {
        super(ErrorCode.AUTHORIZATION_FAILED, message);
    }
}

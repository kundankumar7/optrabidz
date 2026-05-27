package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class SettlementNotPayableException extends ApiException {
    public SettlementNotPayableException(String message) {
        super(ErrorCode.SETTLEMENT_NOT_PAYABLE, message);
    }
}

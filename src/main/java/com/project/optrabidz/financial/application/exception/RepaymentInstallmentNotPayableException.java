package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class RepaymentInstallmentNotPayableException extends ApiException {
    public RepaymentInstallmentNotPayableException(String message) {
        super(ErrorCode.INSTALLMENT_NOT_PAYABLE, message);
    }
}

package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class ReceivingAccountNotReadyException extends ApplicationException {
    public ReceivingAccountNotReadyException(String diagnosticMessage) {
        super(
                FinancialErrors.RECEIVING_ACCOUNT_NOT_READY,
                "FINANCIAL.RECEIVING_ACCOUNT.NOT_READY",
                diagnosticMessage
        );
    }
}

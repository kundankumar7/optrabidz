package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class FinancialOperationNotAllowedException extends ApplicationException {
    public FinancialOperationNotAllowedException(String diagnosticMessage) {
        super(
                FinancialErrors.FINANCIAL_OPERATION_NOT_ALLOWED,
                "FINANCIAL.OPERATION.NOT.ALLOWED",
                diagnosticMessage
        );
    }
}

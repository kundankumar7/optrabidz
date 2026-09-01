package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class RepaymentNotFoundException extends ApplicationException {
    public RepaymentNotFoundException(String diagnosticMessage) {
        super(
                FinancialErrors.REPAYMENT_NOT_FOUND,
                "FINANCIAL.REPAYMENT.NOT.FOUND",
                diagnosticMessage
        );
    }
}

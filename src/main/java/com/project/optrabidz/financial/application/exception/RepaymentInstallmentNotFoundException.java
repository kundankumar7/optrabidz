package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class RepaymentInstallmentNotFoundException extends ApplicationException {
    public RepaymentInstallmentNotFoundException(String diagnosticMessage) {
        super(
                FinancialErrors.REPAYMENT_INSTALLMENT_NOT_FOUND,
                "FINANCIAL.REPAYMENT.INSTALLMENT.NOT.FOUND",
                diagnosticMessage
        );
    }
}

package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class RepaymentInstallmentNotPayableException extends ApplicationException {
    public RepaymentInstallmentNotPayableException(String diagnosticMessage) {
        super(
                FinancialErrors.REPAYMENT_INSTALLMENT_NOT_PAYABLE,
                "FINANCIAL.REPAYMENT.INSTALLMENT.NOT.PAYABLE",
                diagnosticMessage
        );
    }
}

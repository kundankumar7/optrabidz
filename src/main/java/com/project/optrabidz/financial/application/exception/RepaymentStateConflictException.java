package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class RepaymentStateConflictException extends ApplicationException {
    public RepaymentStateConflictException(String diagnosticMessage) {
        super(
                FinancialErrors.REPAYMENT_STATE_CONFLICT,
                "FINANCIAL.REPAYMENT.STATE.CONFLICT",
                diagnosticMessage
        );
    }
}

package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentAccountBindingStateConflictException extends ApplicationException {
    public PaymentAccountBindingStateConflictException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_ACCOUNT_BINDING_STATE_CONFLICT,
                "FINANCIAL.PAYMENT_ACCOUNT_BINDING.STATE.CONFLICT",
                diagnosticMessage
        );
    }
}

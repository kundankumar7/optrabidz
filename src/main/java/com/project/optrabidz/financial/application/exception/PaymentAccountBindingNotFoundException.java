package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentAccountBindingNotFoundException extends ApplicationException {
    public PaymentAccountBindingNotFoundException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_ACCOUNT_BINDING_NOT_FOUND,
                "FINANCIAL.PAYMENT_ACCOUNT_BINDING.NOT_FOUND",
                diagnosticMessage
        );
    }
}

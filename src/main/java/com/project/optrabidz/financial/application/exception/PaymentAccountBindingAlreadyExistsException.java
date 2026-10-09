package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentAccountBindingAlreadyExistsException extends ApplicationException {
    public PaymentAccountBindingAlreadyExistsException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_ACCOUNT_BINDING_ALREADY_EXISTS,
                "FINANCIAL.PAYMENT_ACCOUNT_BINDING.ALREADY_EXISTS",
                diagnosticMessage
        );
    }
}

package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentAlreadyConfirmedException extends ApplicationException {
    public PaymentAlreadyConfirmedException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_ALREADY_CONFIRMED,
                "FINANCIAL.PAYMENT.ALREADY.CONFIRMED",
                diagnosticMessage
        );
    }
}

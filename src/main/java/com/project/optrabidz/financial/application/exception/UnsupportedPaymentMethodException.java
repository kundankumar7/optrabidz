package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class UnsupportedPaymentMethodException extends ApplicationException {
    public UnsupportedPaymentMethodException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_METHOD_UNSUPPORTED,
                "FINANCIAL.PAYMENT.METHOD.UNSUPPORTED",
                diagnosticMessage
        );
    }
}

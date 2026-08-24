package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentProviderMismatchException extends ApplicationException {
    public PaymentProviderMismatchException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_PROVIDER_MISMATCH,
                "FINANCIAL.PAYMENT.PROVIDER.MISMATCH",
                diagnosticMessage
        );
    }
}

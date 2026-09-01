package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentIntentNotFoundException extends ApplicationException {
    public PaymentIntentNotFoundException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_INTENT_NOT_FOUND,
                "FINANCIAL.PAYMENT.INTENT.NOT_FOUND",
                diagnosticMessage
        );
    }
}

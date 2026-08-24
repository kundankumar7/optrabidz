package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentIntentExpiredException extends ApplicationException {
    public PaymentIntentExpiredException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_INTENT_EXPIRED,
                "FINANCIAL.PAYMENT.INTENT.EXPIRED",
                diagnosticMessage
        );
    }
}

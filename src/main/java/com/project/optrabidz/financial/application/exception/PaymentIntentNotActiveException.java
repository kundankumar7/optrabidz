package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentIntentNotActiveException extends ApplicationException {
    public PaymentIntentNotActiveException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_INTENT_NOT_ACTIVE,
                "FINANCIAL.PAYMENT.INTENT.NOT.ACTIVE",
                diagnosticMessage
        );
    }
}

package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentStateConflictException extends ApplicationException {
    public PaymentStateConflictException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_STATE_CONFLICT,
                "FINANCIAL.PAYMENT.STATE.CONFLICT",
                diagnosticMessage
        );
    }

    public PaymentStateConflictException(
            String diagnosticMessage,
            Throwable cause
    ) {
        super(
                FinancialErrors.PAYMENT_STATE_CONFLICT,
                "FINANCIAL.PAYMENT.STATE.CONFLICT",
                diagnosticMessage,
                cause
        );
    }
}

package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentAttemptNotFoundException extends ApplicationException {
    public PaymentAttemptNotFoundException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_ATTEMPT_NOT_FOUND,
                "FINANCIAL.PAYMENT.ATTEMPT.NOT_FOUND",
                diagnosticMessage
        );
    }
}

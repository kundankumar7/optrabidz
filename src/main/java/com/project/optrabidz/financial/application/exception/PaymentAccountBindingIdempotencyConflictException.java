package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentAccountBindingIdempotencyConflictException extends ApplicationException {
    public PaymentAccountBindingIdempotencyConflictException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYMENT_ACCOUNT_BINDING_IDEMPOTENCY_CONFLICT,
                "FINANCIAL.PAYMENT_ACCOUNT_BINDING.IDEMPOTENCY.CONFLICT",
                diagnosticMessage
        );
    }
}

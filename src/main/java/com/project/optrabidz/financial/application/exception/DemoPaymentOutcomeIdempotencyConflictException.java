package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class DemoPaymentOutcomeIdempotencyConflictException extends ApplicationException {
    public DemoPaymentOutcomeIdempotencyConflictException() {
        super(
                FinancialErrors.DEMO_PAYMENT_OUTCOME_IDEMPOTENCY_CONFLICT,
                "FINANCIAL.DEMO_PAYMENT.OUTCOME.IDEMPOTENCY_CONFLICT",
                "The demo outcome idempotency key was reused with different outcome content"
        );
    }
}

package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentWebhookReplayStateException
        extends ApplicationException {
    public PaymentWebhookReplayStateException() {
        super(
                FinancialErrors.PAYMENT_WEBHOOK_PROCESSING_FAILED,
                "FINANCIAL.WEBHOOK.REPLAY.STATE_INVARIANT",
                "Payment webhook replay state invariant failed"
        );
    }
}

package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentWebhookReplayCollisionException
        extends ApplicationException {
    public PaymentWebhookReplayCollisionException() {
        super(
                FinancialErrors.PAYMENT_WEBHOOK_PAYLOAD_INVALID,
                "FINANCIAL.WEBHOOK.PAYLOAD.REPLAY_COLLISION",
                "Authenticated webhook event identity collision"
        );
    }
}

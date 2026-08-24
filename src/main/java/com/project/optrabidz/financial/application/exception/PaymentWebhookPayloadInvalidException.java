package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentWebhookPayloadInvalidException extends ApplicationException {
    public PaymentWebhookPayloadInvalidException(String reason) {
        super(
                FinancialErrors.PAYMENT_WEBHOOK_PAYLOAD_INVALID,
                "FINANCIAL.WEBHOOK.PAYLOAD." + reason,
                "Webhook payload invalid: " + reason
        );
    }

    public PaymentWebhookPayloadInvalidException(String reason, Throwable cause) {
        super(
                FinancialErrors.PAYMENT_WEBHOOK_PAYLOAD_INVALID,
                "FINANCIAL.WEBHOOK.PAYLOAD." + reason,
                "Webhook payload invalid: " + reason,
                cause
        );
    }
}

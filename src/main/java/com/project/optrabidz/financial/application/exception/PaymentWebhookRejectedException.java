package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PaymentWebhookRejectedException extends ApplicationException {
    public PaymentWebhookRejectedException(PaymentWebhookRejectionReason reason) {
        super(
                FinancialErrors.PAYMENT_WEBHOOK_REJECTED,
                "FINANCIAL.WEBHOOK." + reason.name(),
                "Webhook rejected: " + reason.name()
        );
    }

    public PaymentWebhookRejectedException(PaymentWebhookRejectionReason reason, Throwable cause) {
        super(
                FinancialErrors.PAYMENT_WEBHOOK_REJECTED,
                "FINANCIAL.WEBHOOK." + reason.name(),
                "Webhook rejected: " + reason.name(),
                cause
        );
    }
}

package com.project.optrabidz.financial.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

public final class FinancialErrors {
    public static final ErrorDescriptor PAYMENT_WEBHOOK_REJECTED = new ErrorDescriptor(
            "PAYMENT_WEBHOOK_REJECTED",
            ErrorCategory.VALIDATION,
            "The webhook request was rejected"
    );

    public static final ErrorDescriptor PAYMENT_WEBHOOK_PAYLOAD_INVALID = new ErrorDescriptor(
            "PAYMENT_WEBHOOK_PAYLOAD_INVALID",
            ErrorCategory.VALIDATION,
            "The webhook payload is invalid"
    );

    private FinancialErrors() {
    }
}

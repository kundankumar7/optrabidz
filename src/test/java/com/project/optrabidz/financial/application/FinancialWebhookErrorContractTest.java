package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.financial.application.error.FinancialErrors;
import com.project.optrabidz.financial.application.exception.PaymentWebhookPayloadInvalidException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectedException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookRejectionReason;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialWebhookErrorContractTest {
    @Test
    void webhookRejectionUsesOneSafeValidationDescriptor() {
        assertThat(FinancialErrors.PAYMENT_WEBHOOK_REJECTED)
                .isEqualTo(new ErrorDescriptor(
                        "PAYMENT_WEBHOOK_REJECTED",
                        ErrorCategory.VALIDATION,
                        "The webhook request was rejected"
                ));

        PaymentWebhookRejectedException exception = new PaymentWebhookRejectedException(
                PaymentWebhookRejectionReason.SIGNATURE_INVALID
        );
        assertThat(exception).isInstanceOf(ApplicationException.class);
        assertThat(exception.descriptor()).isEqualTo(FinancialErrors.PAYMENT_WEBHOOK_REJECTED);
        assertThat(exception.descriptor().publicMessage()).doesNotContain("signature");
    }

    @Test
    void authenticatedPayloadFailureUsesSafeValidationDescriptor() {
        assertThat(FinancialErrors.PAYMENT_WEBHOOK_PAYLOAD_INVALID)
                .isEqualTo(new ErrorDescriptor(
                        "PAYMENT_WEBHOOK_PAYLOAD_INVALID",
                        ErrorCategory.VALIDATION,
                        "The webhook payload is invalid"
                ));

        PaymentWebhookPayloadInvalidException exception =
                new PaymentWebhookPayloadInvalidException("SCHEMA_INVALID");
        assertThat(exception).isInstanceOf(ApplicationException.class);
        assertThat(exception.descriptor()).isEqualTo(FinancialErrors.PAYMENT_WEBHOOK_PAYLOAD_INVALID);
        assertThat(exception.descriptor().publicMessage()).doesNotContain("SCHEMA_INVALID");
    }
}

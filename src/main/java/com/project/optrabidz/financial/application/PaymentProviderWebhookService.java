package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import org.springframework.stereotype.Service;

@Service
public class PaymentProviderWebhookService {
    private static final String SAFE_PROVIDER_FAILURE_CODE =
            "PROVIDER_REPORTED_FAILURE";
    private static final String SAFE_PROVIDER_FAILURE_MESSAGE =
            "Payment provider reported that the payment failed";

    private final FinancialService financialService;

    public PaymentProviderWebhookService(FinancialService financialService) {
        this.financialService = financialService;
    }

    public PaymentAttemptResponse handle(PaymentProviderWebhookCommand command) {
        if (command.eventType() == PaymentProviderWebhookEventType.PAYMENT_CONFIRMED) {
            return financialService.confirmProviderPaymentAttempt(
                    command.providerCode(),
                    command.paymentAttemptId(),
                    command.providerPaymentId()
            );
        }

        return financialService.failProviderPaymentAttempt(
                command.providerCode(),
                command.paymentAttemptId(),
                SAFE_PROVIDER_FAILURE_CODE,
                SAFE_PROVIDER_FAILURE_MESSAGE
        );
    }
}

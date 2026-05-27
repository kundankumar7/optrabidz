package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEventType;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifier;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifierRegistry;
import org.springframework.stereotype.Service;

@Service
public class PaymentProviderWebhookService {
    private final PaymentProviderWebhookSignatureVerifierRegistry signatureVerifierRegistry;
    private final FinancialService financialService;

    public PaymentProviderWebhookService(PaymentProviderWebhookSignatureVerifierRegistry signatureVerifierRegistry,
                                         FinancialService financialService) {
        this.signatureVerifierRegistry = signatureVerifierRegistry;
        this.financialService = financialService;
    }

    public PaymentAttemptResponse handle(PaymentProviderWebhookCommand command) {
        PaymentProviderWebhookSignatureVerifier verifier = signatureVerifierRegistry.resolve(command.providerCode());
        verifier.verify(command);

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
                command.failureCode(),
                command.failureMessage()
        );
    }
}

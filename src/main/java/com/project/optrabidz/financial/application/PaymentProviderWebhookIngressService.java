package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookEventParser;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifier;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifierRegistry;
import org.springframework.stereotype.Service;

@Service
public class PaymentProviderWebhookIngressService {
    private final PaymentProviderWebhookSignatureVerifierRegistry signatureVerifierRegistry;
    private final PaymentProviderWebhookEventParser eventParser;
    private final PaymentProviderWebhookService webhookService;

    public PaymentProviderWebhookIngressService(
            PaymentProviderWebhookSignatureVerifierRegistry signatureVerifierRegistry,
            PaymentProviderWebhookEventParser eventParser,
            PaymentProviderWebhookService webhookService) {
        this.signatureVerifierRegistry = signatureVerifierRegistry;
        this.eventParser = eventParser;
        this.webhookService = webhookService;
    }

    public PaymentAttemptResponse handle(PaymentProviderWebhookEnvelope envelope) {
        PaymentProviderWebhookSignatureVerifier verifier =
                signatureVerifierRegistry.resolve(envelope.providerCode());
        verifier.verify(envelope);
        PaymentProviderWebhookCommand command = eventParser.parse(
                envelope.providerCode(),
                envelope.rawBody()
        );
        return webhookService.handle(command);
    }
}

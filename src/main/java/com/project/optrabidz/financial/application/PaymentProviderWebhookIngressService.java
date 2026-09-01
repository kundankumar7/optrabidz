package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookEventParser;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifier;
import com.project.optrabidz.financial.application.port.PaymentProviderWebhookSignatureVerifierRegistry;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayEvent;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayFingerprintFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentProviderWebhookIngressService {
    private final PaymentProviderWebhookSignatureVerifierRegistry signatureVerifierRegistry;
    private final PaymentProviderWebhookEventParser eventParser;
    private final PaymentWebhookReplayFingerprintFactory fingerprintFactory;
    private final PaymentWebhookReplayService replayService;

    public PaymentProviderWebhookIngressService(
            PaymentProviderWebhookSignatureVerifierRegistry signatureVerifierRegistry,
            PaymentProviderWebhookEventParser eventParser,
            PaymentWebhookReplayFingerprintFactory fingerprintFactory,
            PaymentWebhookReplayService replayService) {
        this.signatureVerifierRegistry = signatureVerifierRegistry;
        this.eventParser = eventParser;
        this.fingerprintFactory = fingerprintFactory;
        this.replayService = replayService;
    }

    public void handle(PaymentProviderWebhookEnvelope envelope) {
        PaymentProviderWebhookSignatureVerifier verifier =
                signatureVerifierRegistry.resolve(envelope.providerCode());
        verifier.verify(envelope);
        PaymentProviderWebhookCommand command = eventParser.parse(
                envelope.providerCode(),
                envelope.rawBody()
        );
        PaymentWebhookReplayEvent replayEvent = fingerprintFactory.create(command);
        replayService.handle(command, replayEvent);
    }
}

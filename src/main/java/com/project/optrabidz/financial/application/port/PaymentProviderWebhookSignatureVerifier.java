package com.project.optrabidz.financial.application.port;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.command.PaymentProviderWebhookEnvelope;

public interface PaymentProviderWebhookSignatureVerifier {
    boolean supports(String providerCode);

    void verify(PaymentProviderWebhookEnvelope envelope);

    default void verify(PaymentProviderWebhookCommand command) {
        throw new UnsupportedOperationException("Use the pre-authentication webhook envelope");
    }
}

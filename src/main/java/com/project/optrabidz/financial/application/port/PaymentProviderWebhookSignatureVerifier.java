package com.project.optrabidz.financial.application.port;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;

public interface PaymentProviderWebhookSignatureVerifier {
    boolean supports(String providerCode);

    void verify(PaymentProviderWebhookCommand command);
}

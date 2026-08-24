package com.project.optrabidz.financial.application.command;

import java.util.Objects;

public record PaymentProviderWebhookEnvelope(
        String providerCode,
        byte[] rawBody,
        String timestamp,
        String signature
) {
    public PaymentProviderWebhookEnvelope {
        Objects.requireNonNull(providerCode, "providerCode must not be null");
        rawBody = Objects.requireNonNull(rawBody, "rawBody must not be null").clone();
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
    }

    @Override
    public byte[] rawBody() {
        return rawBody.clone();
    }
}

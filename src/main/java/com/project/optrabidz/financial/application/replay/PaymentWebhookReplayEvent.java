package com.project.optrabidz.financial.application.replay;

import java.util.Objects;

public record PaymentWebhookReplayEvent(
        PaymentWebhookReplayContent content,
        String payloadHash
) {
    public PaymentWebhookReplayEvent {
        Objects.requireNonNull(content, "content must not be null");
        if (payloadHash == null || !payloadHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("payloadHash must be a lowercase SHA-256 value");
        }
    }
}

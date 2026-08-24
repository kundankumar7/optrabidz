package com.project.optrabidz.financial.application.replay;

import java.util.Objects;

public record StoredPaymentWebhookReplayEvent(
        long replayEventId,
        PaymentWebhookReplayState state,
        PaymentWebhookReplayEvent event
) {
    public StoredPaymentWebhookReplayEvent {
        if (replayEventId <= 0) {
            throw new IllegalArgumentException("replayEventId must be positive");
        }
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(event, "event must not be null");
    }
}

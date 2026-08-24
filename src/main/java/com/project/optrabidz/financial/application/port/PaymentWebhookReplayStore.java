package com.project.optrabidz.financial.application.port;

import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayEvent;
import com.project.optrabidz.financial.application.replay.StoredPaymentWebhookReplayEvent;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

public interface PaymentWebhookReplayStore {
    OptionalLong tryClaim(PaymentWebhookReplayEvent event, Instant receivedAt);

    Optional<StoredPaymentWebhookReplayEvent> findByIdentity(
            String providerCode,
            String providerEventId
    );

    void markProcessed(
            long replayEventId,
            long paymentIntentId,
            long paymentAttemptId,
            Instant processedAt
    );
}

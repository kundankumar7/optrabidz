package com.project.optrabidz.financial.application;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;
import com.project.optrabidz.financial.application.dto.response.PaymentAttemptResponse;
import com.project.optrabidz.financial.application.exception.PaymentWebhookReplayCollisionException;
import com.project.optrabidz.financial.application.exception.PaymentWebhookReplayStateException;
import com.project.optrabidz.financial.application.port.PaymentWebhookReplayStore;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayContent;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayEvent;
import com.project.optrabidz.financial.application.replay.PaymentWebhookReplayState;
import com.project.optrabidz.financial.application.replay.StoredPaymentWebhookReplayEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.OptionalLong;

@Service
public class PaymentWebhookReplayService {
    private final PaymentWebhookReplayStore replayStore;
    private final PaymentProviderWebhookService webhookService;

    public PaymentWebhookReplayService(
            PaymentWebhookReplayStore replayStore,
            PaymentProviderWebhookService webhookService) {
        this.replayStore = replayStore;
        this.webhookService = webhookService;
    }

    @Transactional
    public void handle(
            PaymentProviderWebhookCommand command,
            PaymentWebhookReplayEvent replayEvent) {
        OptionalLong claim = replayStore.tryClaim(replayEvent, Instant.now());
        if (claim.isEmpty()) {
            classifyExisting(replayEvent);
            return;
        }

        PaymentAttemptResponse response = webhookService.handle(command);
        replayStore.markProcessed(
                claim.getAsLong(),
                response.paymentIntentId(),
                response.paymentAttemptId(),
                Instant.now()
        );
    }

    private void classifyExisting(PaymentWebhookReplayEvent incomingEvent) {
        PaymentWebhookReplayContent content = incomingEvent.content();
        StoredPaymentWebhookReplayEvent stored = replayStore.findByIdentity(
                        content.providerCode(),
                        content.providerEventId()
                )
                .orElseThrow(this::stateInvariant);
        if (stored.state() != PaymentWebhookReplayState.PROCESSED) {
            throw stateInvariant();
        }
        if (!stored.event().equals(incomingEvent)) {
            throw new PaymentWebhookReplayCollisionException();
        }
    }

    private PaymentWebhookReplayStateException stateInvariant() {
        return new PaymentWebhookReplayStateException();
    }
}

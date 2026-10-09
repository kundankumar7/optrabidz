package com.project.optrabidz.financial.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record PayoutTransferFailedEvent(
        Long payoutTransferId,
        Long paymentIntentId,
        Long payerAccountId,
        Long payeeAccountId,
        int attemptCount,
        String failureCode,
        String failureMessage,
        Instant occurredAt
) implements DomainEvent {
}

package com.project.optrabidz.financial.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record PayoutTransferCreatedEvent(
        Long payoutTransferId,
        Long paymentIntentId,
        Long paymentAccountBindingId,
        Instant occurredAt
) implements DomainEvent {
}

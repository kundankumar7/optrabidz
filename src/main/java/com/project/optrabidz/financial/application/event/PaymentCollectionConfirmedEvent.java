package com.project.optrabidz.financial.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record PaymentCollectionConfirmedEvent(
        Long paymentIntentId,
        Instant occurredAt
) implements DomainEvent {
}

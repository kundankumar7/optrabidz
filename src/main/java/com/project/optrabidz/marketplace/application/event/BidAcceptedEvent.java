package com.project.optrabidz.marketplace.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record BidAcceptedEvent(
        Long bidId,
        Long listingId,
        Long investorId,
        Long actorAccountId,
        Instant occurredAt
) implements DomainEvent {
}

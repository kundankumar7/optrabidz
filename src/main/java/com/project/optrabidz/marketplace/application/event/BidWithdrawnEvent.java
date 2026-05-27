package com.project.optrabidz.marketplace.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record BidWithdrawnEvent(
        Long bidId,
        Long listingId,
        Long investorId,
        Long actorAccountId,
        String reason,
        Instant occurredAt
) implements DomainEvent {
}

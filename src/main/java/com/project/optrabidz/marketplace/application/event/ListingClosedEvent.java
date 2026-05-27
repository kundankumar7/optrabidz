package com.project.optrabidz.marketplace.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record ListingClosedEvent(
        Long listingId,
        Long startupId,
        Long actorAccountId,
        String reason,
        Instant occurredAt
) implements DomainEvent {
}

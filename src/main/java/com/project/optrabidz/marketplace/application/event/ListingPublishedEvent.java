package com.project.optrabidz.marketplace.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record ListingPublishedEvent(
        Long listingId,
        Long startupId,
        Long actorAccountId,
        Instant occurredAt
) implements DomainEvent {
}

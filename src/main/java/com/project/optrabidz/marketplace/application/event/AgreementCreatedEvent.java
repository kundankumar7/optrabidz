package com.project.optrabidz.marketplace.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record AgreementCreatedEvent(
        Long agreementId,
        Long listingId,
        Long bidId,
        Long startupId,
        Long investorId,
        Long actorAccountId,
        Instant occurredAt
) implements DomainEvent {
}

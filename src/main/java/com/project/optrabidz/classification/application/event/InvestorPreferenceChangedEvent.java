package com.project.optrabidz.classification.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record InvestorPreferenceChangedEvent(
        Long investorId,
        Long accountId,
        Instant occurredAt
) implements DomainEvent {
    public InvestorPreferenceChangedEvent {
        Objects.requireNonNull(investorId, "investorId must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}

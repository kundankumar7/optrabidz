package com.project.optrabidz.classification.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;

public record StartupClassificationChangedEvent(
        Long startupId,
        Long accountId,
        Instant occurredAt
) implements DomainEvent {
    public StartupClassificationChangedEvent {
        Objects.requireNonNull(startupId, "startupId must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}

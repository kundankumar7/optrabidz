package com.project.optrabidz.participation.application.event;

import com.project.optrabidz.common.event.DomainEvent;
import com.project.optrabidz.identity.domain.model.RoleType;

import java.time.Instant;
import java.util.Objects;

public record ParticipationProfileChangedEvent(
        Long accountId,
        RoleType roleType,
        Instant occurredAt
) implements DomainEvent {

    public ParticipationProfileChangedEvent {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(roleType, "roleType must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}

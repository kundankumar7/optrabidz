package com.project.optrabidz.common.event;

import com.project.optrabidz.identity.domain.model.RoleType;

import java.time.Instant;
import java.util.Objects;

public record AccountRegisteredEvent(
        Long accountId,
        RoleType roleType,
        Instant occurredAt
) implements DomainEvent {

    public AccountRegisteredEvent {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(roleType, "roleType must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}

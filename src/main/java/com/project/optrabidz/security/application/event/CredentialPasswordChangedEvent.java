package com.project.optrabidz.security.application.event;

import com.project.optrabidz.common.event.DomainEvent;
import com.project.optrabidz.identity.domain.model.RoleType;

import java.time.Instant;
import java.util.Objects;

public record CredentialPasswordChangedEvent(
        Long accountId,
        RoleType actorRole,
        int terminatedSessionCount,
        Instant occurredAt
) implements DomainEvent {

    public CredentialPasswordChangedEvent {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(actorRole, "actorRole must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (terminatedSessionCount < 0) {
            throw new IllegalArgumentException("terminatedSessionCount must not be negative");
        }
    }
}

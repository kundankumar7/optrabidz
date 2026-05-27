package com.project.optrabidz.governance.application.admin.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record AdminAuthorityTransferredEvent(
        Long newAdminAccountId,
        Long revokedAdminAccountId,
        Long revokedByAccountId,
        String revocationReason,
        Instant occurredAt
) implements DomainEvent {
}

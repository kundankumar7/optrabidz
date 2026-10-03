package com.project.optrabidz.financial.application.event;

import com.project.optrabidz.common.event.DomainEvent;
import com.project.optrabidz.identity.domain.model.RoleType;

import java.time.Instant;

public record PayoutTransferRetryStartedEvent(
        Long payoutTransferId,
        Long paymentIntentId,
        int attemptCount,
        Long actorAccountId,
        RoleType actorRole,
        Instant occurredAt
) implements DomainEvent {
}

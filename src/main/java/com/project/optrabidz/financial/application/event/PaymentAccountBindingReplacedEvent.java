package com.project.optrabidz.financial.application.event;

import com.project.optrabidz.common.event.DomainEvent;
import com.project.optrabidz.identity.domain.model.RoleType;

import java.time.Instant;

public record PaymentAccountBindingReplacedEvent(
        Long previousPaymentAccountBindingId,
        Long replacementPaymentAccountBindingId,
        Long actorAccountId,
        RoleType actorRole,
        Instant occurredAt
) implements DomainEvent {
    public Long paymentAccountBindingId() {
        return replacementPaymentAccountBindingId;
    }
}

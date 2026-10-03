package com.project.optrabidz.financial.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record PayoutTransferConfirmedEvent(
        Long payoutTransferId,
        Long paymentIntentId,
        Long payerAccountId,
        Long payeeAccountId,
        String providerTransferReference,
        Instant occurredAt
) implements DomainEvent {
}

package com.project.optrabidz.financial.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record RepaymentInstallmentPaymentFailedEvent(
        Long repaymentInstallmentId,
        Long repaymentId,
        Long agreementId,
        Long startupId,
        Long investorId,
        Long paymentIntentId,
        Long actorAccountId,
        String reason,
        Instant occurredAt
) implements DomainEvent {
}

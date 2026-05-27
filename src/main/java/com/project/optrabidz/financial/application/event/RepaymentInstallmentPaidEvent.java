package com.project.optrabidz.financial.application.event;

import com.project.optrabidz.common.event.DomainEvent;

import java.time.Instant;

public record RepaymentInstallmentPaidEvent(
        Long repaymentInstallmentId,
        Long repaymentId,
        Long agreementId,
        Long startupId,
        Long investorId,
        Long paymentIntentId,
        Long actorAccountId,
        Instant occurredAt
) implements DomainEvent {
}

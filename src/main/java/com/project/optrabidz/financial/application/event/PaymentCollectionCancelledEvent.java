package com.project.optrabidz.financial.application.event;

import com.project.optrabidz.common.event.DomainEvent;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;

import java.time.Instant;

public record PaymentCollectionCancelledEvent(
        Long paymentAttemptId,
        Long paymentIntentId,
        PaymentPurpose paymentPurpose,
        Long payerAccountId,
        Long repaymentInstallmentId,
        Long repaymentId,
        Long agreementId,
        Long startupId,
        Long investorId,
        String reason,
        Instant occurredAt
) implements DomainEvent {
}

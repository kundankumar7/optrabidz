package com.project.optrabidz.financial.application.payout;

import java.math.BigDecimal;

public record PayoutInstruction(
        Long payoutTransferId,
        String idempotencyKey,
        String externalRecipientReference,
        BigDecimal amount,
        String currencyCode,
        int attemptCount
) {
}

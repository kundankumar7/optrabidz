package com.project.optrabidz.financial.application.dto.response;

import com.project.optrabidz.financial.domain.model.SettlementState;

import java.math.BigDecimal;
import java.time.Instant;

public record SettlementResponse(
        Long settlementId,
        Long agreementId,
        Long startupId,
        Long investorId,
        BigDecimal amount,
        String currencyCode,
        DebtTermsResponse debtTerms,
        SettlementState settlementState,
        Instant createdAt,
        Instant expiresAt,
        Instant confirmedAt,
        Instant failedAt,
        Instant expiredAt,
        Instant cancelledAt,
        String failureReason,
        Long confirmedPaymentIntentId
) {
}

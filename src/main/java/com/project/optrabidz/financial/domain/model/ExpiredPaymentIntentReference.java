package com.project.optrabidz.financial.domain.model;

public record ExpiredPaymentIntentReference(
        Long paymentIntentId,
        PaymentPurpose paymentPurpose,
        Long settlementId,
        Long repaymentInstallmentId
) {}

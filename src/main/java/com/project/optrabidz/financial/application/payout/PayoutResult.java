package com.project.optrabidz.financial.application.payout;

public record PayoutResult(
        PayoutOutcome outcome,
        String providerReference,
        String failureCode,
        String failureMessage
) {
    public static PayoutResult confirmed(String providerReference) {
        return new PayoutResult(PayoutOutcome.CONFIRMED, providerReference, null, null);
    }

    public static PayoutResult failed(String failureCode, String failureMessage) {
        return new PayoutResult(PayoutOutcome.FAILED, null, failureCode, failureMessage);
    }
}

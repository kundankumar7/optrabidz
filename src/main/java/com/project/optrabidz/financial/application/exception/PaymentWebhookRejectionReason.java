package com.project.optrabidz.financial.application.exception;

public enum PaymentWebhookRejectionReason {
    PROVIDER_CODE_INVALID,
    BODY_INVALID,
    BODY_TOO_LARGE,
    AUTHENTICATION_ENVELOPE_INVALID,
    PROVIDER_UNAVAILABLE,
    TIMESTAMP_INVALID,
    TIMESTAMP_STALE,
    SIGNATURE_INVALID
}

package com.project.optrabidz.financial.domain.model;

public enum PaymentState {
    CREATED,
    PAYMENT_PENDING,
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,
    PAYMENT_EXPIRED,
    PAYMENT_CANCELLED
}

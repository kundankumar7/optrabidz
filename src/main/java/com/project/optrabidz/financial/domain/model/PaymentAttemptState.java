package com.project.optrabidz.financial.domain.model;

public enum PaymentAttemptState {
    CREATED,
    INITIATED,
    REQUIRES_ACTION,
    CONFIRMED,
    FAILED,
    EXPIRED,
    CANCELLED
}

package com.project.optrabidz.financial.application.exception;

public final class PayoutDestinationNotReadyException extends RuntimeException {
    public PayoutDestinationNotReadyException(String message) {
        super(message);
    }
}

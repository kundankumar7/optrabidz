package com.project.optrabidz.common.api.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED),
//    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    CREDENTIAL_LOCKED(HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(HttpStatus.UNAUTHORIZED),
    AUTHORIZATION_FAILED(HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    DUPLICATE_OPERATION(HttpStatus.CONFLICT),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT),
    UNSUPPORTED_FUNDING_MODEL(HttpStatus.BAD_REQUEST),
    LISTING_NOT_OPEN(HttpStatus.CONFLICT),
    BID_ALREADY_EXISTS(HttpStatus.CONFLICT),
    BID_ALREADY_ACCEPTED(HttpStatus.CONFLICT),
    GOVERNANCE_RULE_DENIED(HttpStatus.FORBIDDEN),
    SETTLEMENT_NOT_PAYABLE(HttpStatus.CONFLICT),
    INSTALLMENT_NOT_PAYABLE(HttpStatus.CONFLICT),
    PAYMENT_INTENT_NOT_ACTIVE(HttpStatus.CONFLICT),
    PAYMENT_INTENT_EXPIRED(HttpStatus.CONFLICT),
    PAYMENT_ALREADY_CONFIRMED(HttpStatus.CONFLICT),
    CONFLICT(HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}

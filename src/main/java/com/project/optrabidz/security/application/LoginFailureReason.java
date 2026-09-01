package com.project.optrabidz.security.application;

public enum LoginFailureReason {
    UNKNOWN_IDENTITY,
    INVALID_SECRET,
    CREDENTIAL_LOCKED,
    CREDENTIAL_DISABLED,
    ACCOUNT_RESTRICTED
}

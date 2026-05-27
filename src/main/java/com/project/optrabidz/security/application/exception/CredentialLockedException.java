package com.project.optrabidz.security.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class CredentialLockedException extends ApiException {
    public CredentialLockedException(String message) {
        super(ErrorCode.CREDENTIAL_LOCKED, message);
    }
}

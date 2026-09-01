package com.project.optrabidz.identity.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.identity.application.error.IdentityErrors;

public final class AccountStateConflictException extends ApplicationException {

    public AccountStateConflictException(Long accountId, String operation, Throwable cause) {
        super(
                IdentityErrors.ACCOUNT_STATE_CONFLICT,
                "IDENTITY.ACCOUNT.STATE_CONFLICT",
                "Unable to " + operation + " account " + accountId,
                cause
        );
    }
}

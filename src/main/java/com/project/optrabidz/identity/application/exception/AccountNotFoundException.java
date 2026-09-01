package com.project.optrabidz.identity.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.identity.application.error.IdentityErrors;

public final class AccountNotFoundException extends ApplicationException {

    public AccountNotFoundException(Long accountId) {
        super(
                IdentityErrors.ACCOUNT_NOT_FOUND,
                "IDENTITY.ACCOUNT.NOT_FOUND",
                "Account not found: " + accountId
        );
    }
}

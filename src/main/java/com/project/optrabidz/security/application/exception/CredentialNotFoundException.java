package com.project.optrabidz.security.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.security.application.error.SecurityErrors;

public final class CredentialNotFoundException extends ApplicationException {

    public CredentialNotFoundException(Long accountId) {
        super(
                SecurityErrors.CREDENTIAL_NOT_FOUND,
                "SECURITY.CREDENTIAL.NOT_FOUND",
                "Credential not found for account " + accountId
        );
    }
}

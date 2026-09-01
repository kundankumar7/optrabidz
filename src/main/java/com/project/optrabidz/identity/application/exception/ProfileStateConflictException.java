package com.project.optrabidz.identity.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.identity.application.error.IdentityErrors;

public final class ProfileStateConflictException extends ApplicationException {

    public ProfileStateConflictException(Long accountId, String operation, Throwable cause) {
        super(
                IdentityErrors.PROFILE_STATE_CONFLICT,
                "IDENTITY.PROFILE.STATE_CONFLICT",
                "Unable to " + operation + " profile for account " + accountId,
                cause
        );
    }
}

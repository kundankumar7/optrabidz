package com.project.optrabidz.participation.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.participation.application.error.AdminErrors;

public final class AdminAuthorityAlreadyGrantedException extends ApplicationException {

    public AdminAuthorityAlreadyGrantedException(Long accountId) {
        super(
                AdminErrors.ADMIN_AUTHORITY_ALREADY_GRANTED,
                "PARTICIPATION.ADMIN.AUTHORITY_ALREADY_GRANTED",
                "Administrator authority already granted to account " + accountId
        );
    }
}

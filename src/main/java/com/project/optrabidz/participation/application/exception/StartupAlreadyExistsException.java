package com.project.optrabidz.participation.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.participation.application.error.StartupErrors;

public final class StartupAlreadyExistsException extends ApplicationException {

    public StartupAlreadyExistsException(Long accountId) {
        super(
                StartupErrors.STARTUP_ALREADY_EXISTS,
                "PARTICIPATION.STARTUP.ALREADY_EXISTS",
                "Startup profile already exists for account " + accountId
        );
    }
}

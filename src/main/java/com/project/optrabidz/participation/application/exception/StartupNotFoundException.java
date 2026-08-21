package com.project.optrabidz.participation.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.participation.application.error.StartupErrors;

public final class StartupNotFoundException extends ApplicationException {

    public StartupNotFoundException(Long accountId) {
        this("account", accountId);
    }

    public StartupNotFoundException(String referenceType, Long referenceId) {
        super(
                StartupErrors.STARTUP_NOT_FOUND,
                "PARTICIPATION.STARTUP.NOT_FOUND",
                "Startup profile not found for " + referenceType + " " + referenceId
        );
    }
}

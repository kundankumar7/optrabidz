package com.project.optrabidz.classification.application.exception;

import com.project.optrabidz.classification.application.error.ClassificationErrors;
import com.project.optrabidz.common.error.ApplicationException;

public final class StartupClassificationProfileRequiredException extends ApplicationException {

    public StartupClassificationProfileRequiredException(Long accountId) {
        super(
                ClassificationErrors.STARTUP_CLASSIFICATION_PROFILE_REQUIRED,
                "CLASSIFICATION.STARTUP.PROFILE_REQUIRED",
                "Startup profile required for account " + accountId
        );
    }
}

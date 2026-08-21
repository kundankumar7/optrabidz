package com.project.optrabidz.classification.application.exception;

import com.project.optrabidz.classification.application.error.ClassificationErrors;
import com.project.optrabidz.common.error.ApplicationException;

public final class StartupClassificationAlreadyExistsException extends ApplicationException {

    public StartupClassificationAlreadyExistsException(String type, String value) {
        super(
                ClassificationErrors.STARTUP_CLASSIFICATION_ALREADY_EXISTS,
                "CLASSIFICATION.STARTUP.ALREADY_EXISTS",
                "Startup classification already exists: " + type + "=" + value
        );
    }
}

package com.project.optrabidz.classification.application.exception;

import com.project.optrabidz.classification.application.error.ClassificationErrors;
import com.project.optrabidz.common.error.ApplicationException;

public final class StartupClassificationNotFoundException extends ApplicationException {

    public StartupClassificationNotFoundException(String type, String value) {
        super(
                ClassificationErrors.STARTUP_CLASSIFICATION_NOT_FOUND,
                "CLASSIFICATION.STARTUP.NOT_FOUND",
                "Startup classification not found: " + type + "=" + value
        );
    }
}

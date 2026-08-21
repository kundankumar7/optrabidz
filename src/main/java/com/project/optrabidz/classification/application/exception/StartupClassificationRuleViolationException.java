package com.project.optrabidz.classification.application.exception;

import com.project.optrabidz.classification.application.error.ClassificationErrors;
import com.project.optrabidz.common.error.ApplicationException;

public final class StartupClassificationRuleViolationException extends ApplicationException {

    public StartupClassificationRuleViolationException(String diagnosticMessage) {
        super(
                ClassificationErrors.STARTUP_CLASSIFICATION_RULE_VIOLATION,
                "CLASSIFICATION.STARTUP.RULE_VIOLATION",
                diagnosticMessage
        );
    }
}

package com.project.optrabidz.classification.application.exception;

import com.project.optrabidz.classification.application.error.ClassificationErrors;
import com.project.optrabidz.common.error.ApplicationException;

public final class InvestorPreferenceRuleViolationException extends ApplicationException {

    public InvestorPreferenceRuleViolationException(String diagnosticMessage) {
        super(
                ClassificationErrors.INVESTOR_PREFERENCE_RULE_VIOLATION,
                "CLASSIFICATION.INVESTOR.RULE_VIOLATION",
                diagnosticMessage
        );
    }
}

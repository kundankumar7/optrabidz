package com.project.optrabidz.classification.application.exception;

import com.project.optrabidz.classification.application.error.ClassificationErrors;
import com.project.optrabidz.common.error.ApplicationException;

public final class InvestorPreferenceNotFoundException extends ApplicationException {

    public InvestorPreferenceNotFoundException(String type, String value) {
        super(
                ClassificationErrors.INVESTOR_PREFERENCE_NOT_FOUND,
                "CLASSIFICATION.INVESTOR.NOT_FOUND",
                "Investor preference not found: " + type + "=" + value
        );
    }
}

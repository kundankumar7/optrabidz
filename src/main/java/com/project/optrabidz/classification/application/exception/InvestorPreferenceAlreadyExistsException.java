package com.project.optrabidz.classification.application.exception;

import com.project.optrabidz.classification.application.error.ClassificationErrors;
import com.project.optrabidz.common.error.ApplicationException;

public final class InvestorPreferenceAlreadyExistsException extends ApplicationException {

    public InvestorPreferenceAlreadyExistsException(String type, String value) {
        super(
                ClassificationErrors.INVESTOR_PREFERENCE_ALREADY_EXISTS,
                "CLASSIFICATION.INVESTOR.ALREADY_EXISTS",
                "Investor preference already exists: " + type + "=" + value
        );
    }
}

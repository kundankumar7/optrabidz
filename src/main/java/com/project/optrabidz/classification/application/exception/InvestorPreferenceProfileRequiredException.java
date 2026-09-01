package com.project.optrabidz.classification.application.exception;

import com.project.optrabidz.classification.application.error.ClassificationErrors;
import com.project.optrabidz.common.error.ApplicationException;

public final class InvestorPreferenceProfileRequiredException extends ApplicationException {

    public InvestorPreferenceProfileRequiredException(Long accountId) {
        super(
                ClassificationErrors.INVESTOR_PREFERENCE_PROFILE_REQUIRED,
                "CLASSIFICATION.INVESTOR.PROFILE_REQUIRED",
                "Investor profile required for account " + accountId
        );
    }
}

package com.project.optrabidz.participation.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.participation.application.error.InvestorErrors;

public final class InvestorNotFoundException extends ApplicationException {

    public InvestorNotFoundException(Long accountId) {
        this("account", accountId);
    }

    public InvestorNotFoundException(String referenceType, Long referenceId) {
        super(
                InvestorErrors.INVESTOR_NOT_FOUND,
                "PARTICIPATION.INVESTOR.NOT_FOUND",
                "Investor profile not found for " + referenceType + " " + referenceId
        );
    }
}

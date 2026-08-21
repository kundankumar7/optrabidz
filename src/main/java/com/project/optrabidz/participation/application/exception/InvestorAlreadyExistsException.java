package com.project.optrabidz.participation.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.participation.application.error.InvestorErrors;

public final class InvestorAlreadyExistsException extends ApplicationException {

    public InvestorAlreadyExistsException(Long accountId) {
        super(
                InvestorErrors.INVESTOR_ALREADY_EXISTS,
                "PARTICIPATION.INVESTOR.ALREADY_EXISTS",
                "Investor profile already exists for account " + accountId
        );
    }
}

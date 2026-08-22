package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;

public final class AgreementNotFoundException extends ApplicationException {
    public AgreementNotFoundException(String diagnosticMessage) {
        super(
                MarketplaceErrors.AGREEMENT_NOT_FOUND,
                "MARKETPLACE.AGREEMENT.NOT_FOUND",
                diagnosticMessage
        );
    }
}

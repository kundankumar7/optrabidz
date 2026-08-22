package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;

public final class UnsupportedFundingModelException extends ApplicationException {
    public UnsupportedFundingModelException(String diagnosticMessage) {
        super(
                MarketplaceErrors.UNSUPPORTED_FUNDING_MODEL,
                "MARKETPLACE.FUNDING_MODEL.UNSUPPORTED",
                diagnosticMessage
        );
    }
}

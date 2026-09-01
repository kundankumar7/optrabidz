package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;

public final class MarketplaceAccessException extends ApplicationException {
    public MarketplaceAccessException(String diagnosticMessage) {
        super(
                MarketplaceErrors.MARKETPLACE_ACCESS_DENIED,
                "MARKETPLACE.ACCESS.DENIED",
                diagnosticMessage
        );
    }
}

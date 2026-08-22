package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;

public final class BidNotFoundException extends ApplicationException {
    public BidNotFoundException(String diagnosticMessage) {
        super(
                MarketplaceErrors.BID_NOT_FOUND,
                "MARKETPLACE.BID.NOT_FOUND",
                diagnosticMessage
        );
    }
}

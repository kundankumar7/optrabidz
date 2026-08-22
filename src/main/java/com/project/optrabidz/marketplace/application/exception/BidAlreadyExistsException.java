package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;

public final class BidAlreadyExistsException extends ApplicationException {
    public BidAlreadyExistsException(String diagnosticMessage) {
        super(
                MarketplaceErrors.BID_ALREADY_EXISTS,
                "MARKETPLACE.BID.ALREADY_EXISTS",
                diagnosticMessage
        );
    }
}

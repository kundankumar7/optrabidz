package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;

public final class InvalidBidStateException extends ApplicationException {
    public InvalidBidStateException(String diagnosticMessage) {
        super(
                MarketplaceErrors.BID_STATE_CONFLICT,
                "MARKETPLACE.BID.STATE_CONFLICT",
                diagnosticMessage
        );
    }
}

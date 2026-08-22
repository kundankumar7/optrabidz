package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;

public final class BidAlreadyAcceptedException extends ApplicationException {
    public BidAlreadyAcceptedException(String diagnosticMessage) {
        super(
                MarketplaceErrors.BID_ACCEPTANCE_CONFLICT,
                "MARKETPLACE.BID.ACCEPTANCE_CONFLICT",
                diagnosticMessage
        );
    }
}

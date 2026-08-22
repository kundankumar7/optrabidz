package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;

public final class InvalidListingStateException extends ApplicationException {
    public InvalidListingStateException(String diagnosticMessage) {
        super(
                MarketplaceErrors.LISTING_STATE_CONFLICT,
                "MARKETPLACE.LISTING.STATE_CONFLICT",
                diagnosticMessage
        );
    }
}

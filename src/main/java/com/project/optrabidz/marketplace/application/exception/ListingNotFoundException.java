package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;

public final class ListingNotFoundException extends ApplicationException {
    public ListingNotFoundException(String diagnosticMessage) {
        super(
                MarketplaceErrors.LISTING_NOT_FOUND,
                "MARKETPLACE.LISTING.NOT_FOUND",
                diagnosticMessage
        );
    }
}

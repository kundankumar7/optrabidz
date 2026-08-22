package com.project.optrabidz.marketplace.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;

public final class MarketplaceErrors {
    public static final ErrorDescriptor LISTING_NOT_FOUND = new ErrorDescriptor(
            "LISTING_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested listing was not found"
    );
    public static final ErrorDescriptor BID_NOT_FOUND = new ErrorDescriptor(
            "BID_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested bid was not found"
    );
    public static final ErrorDescriptor AGREEMENT_NOT_FOUND = new ErrorDescriptor(
            "AGREEMENT_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested agreement was not found"
    );
    public static final ErrorDescriptor MARKETPLACE_ACCESS_DENIED = new ErrorDescriptor(
            "MARKETPLACE_ACCESS_DENIED",
            ErrorCategory.AUTHORIZATION,
            "You are not authorized to perform this marketplace action"
    );
    public static final ErrorDescriptor LISTING_STATE_CONFLICT = new ErrorDescriptor(
            "LISTING_STATE_CONFLICT",
            ErrorCategory.CONFLICT,
            "The requested action conflicts with the current listing state"
    );
    public static final ErrorDescriptor BID_STATE_CONFLICT = new ErrorDescriptor(
            "BID_STATE_CONFLICT",
            ErrorCategory.CONFLICT,
            "The requested action conflicts with the current bid state"
    );
    public static final ErrorDescriptor BID_ALREADY_EXISTS = new ErrorDescriptor(
            "BID_ALREADY_EXISTS",
            ErrorCategory.CONFLICT,
            "An active bid already exists for this listing"
    );
    public static final ErrorDescriptor BID_ACCEPTANCE_CONFLICT = new ErrorDescriptor(
            "BID_ACCEPTANCE_CONFLICT",
            ErrorCategory.CONFLICT,
            "The bid cannot be accepted in the current marketplace state"
    );
    public static final ErrorDescriptor UNSUPPORTED_FUNDING_MODEL = new ErrorDescriptor(
            "UNSUPPORTED_FUNDING_MODEL",
            ErrorCategory.BUSINESS_RULE,
            "The requested funding model is not supported"
    );

    private MarketplaceErrors() {
    }
}

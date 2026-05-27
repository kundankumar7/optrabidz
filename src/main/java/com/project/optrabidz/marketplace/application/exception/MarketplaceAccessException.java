package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class MarketplaceAccessException extends ApiException {
    public MarketplaceAccessException(String message) {
        super(ErrorCode.AUTHORIZATION_FAILED, message);
    }
}

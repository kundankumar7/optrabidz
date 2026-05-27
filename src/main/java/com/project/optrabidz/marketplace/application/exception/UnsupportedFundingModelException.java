package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class UnsupportedFundingModelException extends ApiException {
    public UnsupportedFundingModelException(String message) {
        super(ErrorCode.UNSUPPORTED_FUNDING_MODEL, message);
    }
}

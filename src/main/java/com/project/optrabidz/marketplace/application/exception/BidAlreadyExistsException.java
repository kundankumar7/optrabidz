package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class BidAlreadyExistsException extends ApiException {
    public BidAlreadyExistsException(String message) {
        super(ErrorCode.BID_ALREADY_EXISTS, message);
    }
}

package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class BidAlreadyAcceptedException extends ApiException {
    public BidAlreadyAcceptedException(String message) {
        super(ErrorCode.BID_ALREADY_ACCEPTED, message);
    }
}

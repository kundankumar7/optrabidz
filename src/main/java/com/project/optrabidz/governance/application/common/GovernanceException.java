package com.project.optrabidz.governance.application.common;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;

public class GovernanceException extends ApiException {
    public GovernanceException(String message) {
        super(ErrorCode.AUTHORIZATION_FAILED, message);
    }

    public GovernanceException(GovernanceDecision decision) {
        super(ErrorCode.AUTHORIZATION_FAILED, decision.message());
    }
}

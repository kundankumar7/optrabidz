package com.project.optrabidz.marketplace.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;

public final class ReceivingAccountNotReadyException extends ApplicationException {
    public ReceivingAccountNotReadyException(String diagnosticMessage) {
        super(
                MarketplaceErrors.RECEIVING_ACCOUNT_NOT_READY,
                "MARKETPLACE.RECEIVING_ACCOUNT.NOT_READY",
                diagnosticMessage
        );
    }
}

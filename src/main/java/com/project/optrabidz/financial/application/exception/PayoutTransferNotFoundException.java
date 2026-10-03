package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PayoutTransferNotFoundException extends ApplicationException {
    public PayoutTransferNotFoundException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYOUT_TRANSFER_NOT_FOUND,
                "FINANCIAL.PAYOUT.TRANSFER.NOT.FOUND",
                diagnosticMessage
        );
    }
}

package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class PayoutTransferStateConflictException extends ApplicationException {
    public PayoutTransferStateConflictException(String diagnosticMessage) {
        super(
                FinancialErrors.PAYOUT_TRANSFER_STATE_CONFLICT,
                "FINANCIAL.PAYOUT.TRANSFER.STATE.CONFLICT",
                diagnosticMessage
        );
    }
}

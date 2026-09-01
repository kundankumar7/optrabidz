package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class SettlementStateConflictException extends ApplicationException {
    public SettlementStateConflictException(String diagnosticMessage) {
        super(
                FinancialErrors.SETTLEMENT_STATE_CONFLICT,
                "FINANCIAL.SETTLEMENT.STATE.CONFLICT",
                diagnosticMessage
        );
    }
}

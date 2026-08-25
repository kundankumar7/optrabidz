package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class SettlementNotFoundException extends ApplicationException {
    public SettlementNotFoundException(String diagnosticMessage) {
        super(
                FinancialErrors.SETTLEMENT_NOT_FOUND,
                "FINANCIAL.SETTLEMENT.NOT.FOUND",
                diagnosticMessage
        );
    }
}

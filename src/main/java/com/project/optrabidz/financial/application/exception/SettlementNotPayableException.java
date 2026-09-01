package com.project.optrabidz.financial.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.financial.application.error.FinancialErrors;

public final class SettlementNotPayableException extends ApplicationException {
    public SettlementNotPayableException(String diagnosticMessage) {
        super(
                FinancialErrors.SETTLEMENT_NOT_PAYABLE,
                "FINANCIAL.SETTLEMENT.NOT.PAYABLE",
                diagnosticMessage
        );
    }
}

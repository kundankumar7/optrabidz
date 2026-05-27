package com.project.optrabidz.financial.infrastructure.adapter;

import com.project.optrabidz.financial.application.FinancialService;
import com.project.optrabidz.governance.application.port.FinanceLifecycleGovernancePort;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class FinanceLifecycleGovernanceAdapter implements FinanceLifecycleGovernancePort {
    private final FinancialService financialService;

    public FinanceLifecycleGovernanceAdapter(FinancialService financialService) {
        this.financialService = financialService;
    }

    @Override
    public int expirePendingPaymentIntents(Instant now, int batchSize) {
        return financialService.expirePendingPaymentIntents(now, batchSize);
    }

    @Override
    public int expirePendingSettlements(Instant now, int batchSize) {
        return financialService.expirePendingSettlements(now, batchSize);
    }

    @Override
    public int markOverdueRepaymentInstallments(Instant now, int batchSize) {
        return financialService.markOverdueRepaymentInstallments(now, batchSize);
    }
}

package com.project.optrabidz.financial.infrastructure.adapter;

import com.project.optrabidz.financial.application.FinancialService;
import com.project.optrabidz.marketplace.application.port.FinanceAgreementPort;
import com.project.optrabidz.marketplace.domain.model.Agreement;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceFinanceAgreementAdapter implements FinanceAgreementPort {
    private final FinancialService financialService;

    public MarketplaceFinanceAgreementAdapter(FinancialService financialService) {
        this.financialService = financialService;
    }

    @Override
    public void onAgreementCreated(Agreement agreement) {
        financialService.createSettlementForAgreement(agreement);
    }
}

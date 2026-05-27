package com.project.optrabidz.marketplace.infrastructure.adapter;

import com.project.optrabidz.marketplace.application.port.FinanceAgreementPort;
import com.project.optrabidz.marketplace.domain.model.Agreement;

public class NoOpFinanceAgreementAdapter implements FinanceAgreementPort {
    @Override
    public void onAgreementCreated(Agreement agreement) {
        // Finance will plug in here once settlement and repayment workflows are implemented.
    }
}

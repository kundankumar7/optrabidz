package com.project.optrabidz.marketplace.application.port;

import com.project.optrabidz.marketplace.domain.model.Agreement;

public interface FinanceAgreementPort {
    void onAgreementCreated(Agreement agreement);
}

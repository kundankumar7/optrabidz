package com.project.optrabidz.governance.application.port;

import java.time.Instant;

public interface FinanceLifecycleGovernancePort {
    int expirePendingPaymentIntents(Instant now, int batchSize);

    int expirePendingSettlements(Instant now, int batchSize);

    int markOverdueRepaymentInstallments(Instant now, int batchSize);
}

package com.project.optrabidz.financial.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum RepaymentInstallmentPaymentView {
    UNPAID(EnumSet.of(
            RepaymentInstallmentState.NOT_STARTED,
            RepaymentInstallmentState.PAYMENT_IN_PROGRESS,
            RepaymentInstallmentState.PAYMENT_FAILED,
            RepaymentInstallmentState.OVERDUE
    )),
    YET_TO_BE_PAID(EnumSet.of(
            RepaymentInstallmentState.NOT_STARTED,
            RepaymentInstallmentState.PAYMENT_FAILED,
            RepaymentInstallmentState.OVERDUE
    ));

    private final Set<RepaymentInstallmentState> states;

    RepaymentInstallmentPaymentView(Set<RepaymentInstallmentState> states) {
        this.states = Set.copyOf(states);
    }

    public Set<RepaymentInstallmentState> states() {
        return states;
    }
}

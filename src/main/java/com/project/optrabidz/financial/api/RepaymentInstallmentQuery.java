package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.domain.model.RepaymentInstallmentPaymentView;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
@ValidRepaymentInstallmentFilterSelection
public record RepaymentInstallmentQuery(
        RepaymentInstallmentState installmentState,
        RepaymentInstallmentPaymentView paymentView,
        Integer page,
        Integer size
) {
    public RepaymentInstallmentQuery {
        page = page == null || page < 1 ? 1 : page;
        size = size == null || size == 0 ? 20 : size;
    }

}

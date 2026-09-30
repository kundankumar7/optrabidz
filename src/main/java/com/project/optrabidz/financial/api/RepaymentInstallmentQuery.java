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
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    public RepaymentInstallmentQuery {
        page = normalizePage(page);
        size = normalizeSize(size);
    }

    private static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private static int normalizeSize(Integer size) {
        return size == null
                ? DEFAULT_PAGE_SIZE
                : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}

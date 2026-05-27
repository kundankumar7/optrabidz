package com.project.optrabidz.marketplace.domain.model;

import org.springframework.util.Assert;

final class DebtRepaymentPlanRules {
    private DebtRepaymentPlanRules() {
    }

    static void validate(RepaymentPlanType repaymentPlanType,
                         Integer oneTimeRepaymentDueAfterMonths,
                         Integer tenureMonths,
                         String tenureFieldName) {
        Assert.notNull(repaymentPlanType, "repaymentPlanType must not be null");
        if (repaymentPlanType == RepaymentPlanType.ONE_TIME) {
            Assert.notNull(tenureMonths, tenureFieldName + " must not be null for ONE_TIME repayment plan");
            Assert.notNull(oneTimeRepaymentDueAfterMonths,
                    "oneTimeRepaymentDueAfterMonths must not be null for ONE_TIME repayment plan");
            Assert.isTrue(oneTimeRepaymentDueAfterMonths > 0,
                    "oneTimeRepaymentDueAfterMonths must be greater than zero");
            Assert.isTrue(oneTimeRepaymentDueAfterMonths <= tenureMonths,
                    "oneTimeRepaymentDueAfterMonths must be less than or equal to tenure months");
            return;
        }
        Assert.isNull(oneTimeRepaymentDueAfterMonths,
                "oneTimeRepaymentDueAfterMonths must be null for installment repayment plans");
    }
}

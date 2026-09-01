package com.project.optrabidz.financial.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class RepaymentInstallmentFilterSelectionValidator implements
        ConstraintValidator<
                ValidRepaymentInstallmentFilterSelection,
                RepaymentInstallmentQuery
        > {

    @Override
    public boolean isValid(
            RepaymentInstallmentQuery query,
            ConstraintValidatorContext context
    ) {
        return query == null
                || query.installmentState() == null
                || query.paymentView() == null;
    }
}

package com.project.optrabidz.financial.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RepaymentInstallmentFilterSelectionValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRepaymentInstallmentFilterSelection {
    String message() default
            "Use either installmentState or paymentView, not both";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

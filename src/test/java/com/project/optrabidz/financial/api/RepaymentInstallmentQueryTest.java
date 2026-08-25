package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.domain.model.RepaymentInstallmentPaymentView;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RepaymentInstallmentQueryTest {
    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsNoFilterOrOneInstallmentFilter() {
        assertThat(validator.validate(new RepaymentInstallmentQuery(null, null, 1, 20)))
                .isEmpty();
        assertThat(validator.validate(new RepaymentInstallmentQuery(
                RepaymentInstallmentState.NOT_STARTED, null, 1, 20)))
                .isEmpty();
        assertThat(validator.validate(new RepaymentInstallmentQuery(
                null, RepaymentInstallmentPaymentView.UNPAID, 1, 20)))
                .isEmpty();
    }

    @Test
    void rejectsSimultaneousStateAndPaymentViewFilters() {
        RepaymentInstallmentQuery query = new RepaymentInstallmentQuery(
                RepaymentInstallmentState.NOT_STARTED,
                RepaymentInstallmentPaymentView.UNPAID,
                1,
                20
        );

        Set<ConstraintViolation<RepaymentInstallmentQuery>> violations =
                validator.validate(query);

        assertThat(violations)
                .singleElement()
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo("Use either installmentState or paymentView, not both");
    }

    @Test
    void normalizesPaginationDefaultsWithoutChangingExplicitValues() {
        assertThat(new RepaymentInstallmentQuery(null, null, 0, 0))
                .extracting(
                        RepaymentInstallmentQuery::page,
                        RepaymentInstallmentQuery::size
                )
                .containsExactly(1, 20);
        assertThat(new RepaymentInstallmentQuery(null, null, 3, 50))
                .extracting(
                        RepaymentInstallmentQuery::page,
                        RepaymentInstallmentQuery::size
                )
                .containsExactly(3, 50);
    }
}

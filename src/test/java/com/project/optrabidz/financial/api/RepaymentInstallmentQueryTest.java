package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.domain.model.RepaymentInstallmentPaymentView;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    void defaultsPaginationOnlyWhenValuesAreAbsent() {
        assertThat(new RepaymentInstallmentQuery(null, null, null, null))
                .extracting(
                        RepaymentInstallmentQuery::page,
                        RepaymentInstallmentQuery::size
                )
                .containsExactly(1, 20);
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 1",
            "0, 1",
            "1, 1",
            "3, 3"
    })
    void normalizesPageToOneWhenSuppliedValueIsNotPositive(int supplied, int expected) {
        assertThat(new RepaymentInstallmentQuery(null, null, supplied, 20).page())
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 1",
            "0, 1",
            "1, 1",
            "100, 100",
            "101, 100"
    })
    void boundsSuppliedPageSizeBetweenOneAndOneHundred(int supplied, int expected) {
        assertThat(new RepaymentInstallmentQuery(null, null, 1, supplied).size())
                .isEqualTo(expected);
    }

    @Test
    void preservesExplicitPaginationWithinBounds() {
        assertThat(new RepaymentInstallmentQuery(null, null, 3, 50))
                .extracting(
                        RepaymentInstallmentQuery::page,
                        RepaymentInstallmentQuery::size
                )
                .containsExactly(3, 50);
    }
}

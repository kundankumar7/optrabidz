package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.financial.application.exception.RepaymentInstallmentNotFoundException;
import com.project.optrabidz.financial.application.exception.RepaymentInstallmentNotPayableException;
import com.project.optrabidz.financial.application.exception.RepaymentNotFoundException;
import com.project.optrabidz.financial.application.exception.RepaymentStateConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.project.optrabidz.financial.application.error.FinancialErrors.REPAYMENT_INSTALLMENT_NOT_FOUND;
import static com.project.optrabidz.financial.application.error.FinancialErrors.REPAYMENT_INSTALLMENT_NOT_PAYABLE;
import static com.project.optrabidz.financial.application.error.FinancialErrors.REPAYMENT_NOT_FOUND;
import static com.project.optrabidz.financial.application.error.FinancialErrors.REPAYMENT_STATE_CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FinancialRepaymentErrorContractTest {
    private static final String PROTECTED_DIAGNOSTIC =
            "protected-repayment-sentinel";

    @ParameterizedTest
    @MethodSource("descriptors")
    void exposesApprovedRepaymentDescriptors(
            ErrorDescriptor descriptor,
            String code,
            ErrorCategory category,
            String publicMessage
    ) {
        assertThat(descriptor).isEqualTo(new ErrorDescriptor(
                code,
                category,
                publicMessage
        ));
    }

    @ParameterizedTest
    @MethodSource("failures")
    void typedRepaymentFailuresKeepProtectedDiagnosticsOutOfPublicMessages(
            Function<String, ApplicationException> factory,
            ErrorDescriptor descriptor,
            String diagnosticCode
    ) {
        ApplicationException failure = factory.apply(PROTECTED_DIAGNOSTIC);

        assertThat(failure.descriptor()).isSameAs(descriptor);
        assertThat(failure.diagnosticCode()).isEqualTo(diagnosticCode);
        assertThat(failure.getMessage()).contains(PROTECTED_DIAGNOSTIC);
        assertThat(failure.descriptor().publicMessage())
                .doesNotContain(PROTECTED_DIAGNOSTIC);
    }

    private static Stream<Arguments> descriptors() {
        return Stream.of(
                arguments(REPAYMENT_NOT_FOUND,
                        "REPAYMENT_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        "The requested repayment was not found"),
                arguments(REPAYMENT_INSTALLMENT_NOT_FOUND,
                        "REPAYMENT_INSTALLMENT_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        "The requested repayment installment was not found"),
                arguments(REPAYMENT_INSTALLMENT_NOT_PAYABLE,
                        "REPAYMENT_INSTALLMENT_NOT_PAYABLE",
                        ErrorCategory.CONFLICT,
                        "The repayment installment cannot be paid in its current state"),
                arguments(REPAYMENT_STATE_CONFLICT,
                        "REPAYMENT_STATE_CONFLICT",
                        ErrorCategory.CONFLICT,
                        "The repayment state no longer permits this operation")
        );
    }

    private static Stream<Arguments> failures() {
        return Stream.of(
                arguments(
                        (Function<String, ApplicationException>)
                                RepaymentNotFoundException::new,
                        REPAYMENT_NOT_FOUND,
                        "FINANCIAL.REPAYMENT.NOT.FOUND"),
                arguments(
                        (Function<String, ApplicationException>)
                                RepaymentInstallmentNotFoundException::new,
                        REPAYMENT_INSTALLMENT_NOT_FOUND,
                        "FINANCIAL.REPAYMENT.INSTALLMENT.NOT.FOUND"),
                arguments(
                        (Function<String, ApplicationException>)
                                RepaymentInstallmentNotPayableException::new,
                        REPAYMENT_INSTALLMENT_NOT_PAYABLE,
                        "FINANCIAL.REPAYMENT.INSTALLMENT.NOT.PAYABLE"),
                arguments(
                        (Function<String, ApplicationException>)
                                RepaymentStateConflictException::new,
                        REPAYMENT_STATE_CONFLICT,
                        "FINANCIAL.REPAYMENT.STATE.CONFLICT")
        );
    }
}

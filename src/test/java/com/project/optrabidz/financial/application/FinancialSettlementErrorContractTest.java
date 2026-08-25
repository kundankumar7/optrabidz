package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.financial.application.exception.FinancialOperationNotAllowedException;
import com.project.optrabidz.financial.application.exception.SettlementNotFoundException;
import com.project.optrabidz.financial.application.exception.SettlementNotPayableException;
import com.project.optrabidz.financial.application.exception.SettlementStateConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.project.optrabidz.financial.application.error.FinancialErrors.FINANCIAL_OPERATION_NOT_ALLOWED;
import static com.project.optrabidz.financial.application.error.FinancialErrors.SETTLEMENT_NOT_FOUND;
import static com.project.optrabidz.financial.application.error.FinancialErrors.SETTLEMENT_NOT_PAYABLE;
import static com.project.optrabidz.financial.application.error.FinancialErrors.SETTLEMENT_STATE_CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FinancialSettlementErrorContractTest {
    private static final String PROTECTED_DIAGNOSTIC =
            "protected-settlement-sentinel";

    @ParameterizedTest
    @MethodSource("descriptors")
    void exposesApprovedSettlementDescriptors(
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
    void typedSettlementFailuresKeepProtectedDiagnosticsOutOfPublicMessages(
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
                arguments(FINANCIAL_OPERATION_NOT_ALLOWED,
                        "FINANCIAL_OPERATION_NOT_ALLOWED",
                        ErrorCategory.AUTHORIZATION,
                        "This financial operation is not allowed"),
                arguments(SETTLEMENT_NOT_FOUND,
                        "SETTLEMENT_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        "The requested settlement was not found"),
                arguments(SETTLEMENT_NOT_PAYABLE,
                        "SETTLEMENT_NOT_PAYABLE",
                        ErrorCategory.CONFLICT,
                        "The settlement cannot be paid in its current state"),
                arguments(SETTLEMENT_STATE_CONFLICT,
                        "SETTLEMENT_STATE_CONFLICT",
                        ErrorCategory.CONFLICT,
                        "The settlement state no longer permits this operation")
        );
    }

    private static Stream<Arguments> failures() {
        return Stream.of(
                arguments(
                        (Function<String, ApplicationException>)
                                FinancialOperationNotAllowedException::new,
                        FINANCIAL_OPERATION_NOT_ALLOWED,
                        "FINANCIAL.OPERATION.NOT.ALLOWED"),
                arguments(
                        (Function<String, ApplicationException>)
                                SettlementNotFoundException::new,
                        SETTLEMENT_NOT_FOUND,
                        "FINANCIAL.SETTLEMENT.NOT.FOUND"),
                arguments(
                        (Function<String, ApplicationException>)
                                SettlementNotPayableException::new,
                        SETTLEMENT_NOT_PAYABLE,
                        "FINANCIAL.SETTLEMENT.NOT.PAYABLE"),
                arguments(
                        (Function<String, ApplicationException>)
                                SettlementStateConflictException::new,
                        SETTLEMENT_STATE_CONFLICT,
                        "FINANCIAL.SETTLEMENT.STATE.CONFLICT")
        );
    }
}

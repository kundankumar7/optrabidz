package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.financial.application.exception.PaymentAccountBindingAlreadyExistsException;
import com.project.optrabidz.financial.application.exception.PaymentAccountBindingIdempotencyConflictException;
import com.project.optrabidz.financial.application.exception.PaymentAccountBindingNotFoundException;
import com.project.optrabidz.financial.application.exception.PaymentAccountBindingStateConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_ACCOUNT_BINDING_ALREADY_EXISTS;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_ACCOUNT_BINDING_IDEMPOTENCY_CONFLICT;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_ACCOUNT_BINDING_NOT_FOUND;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_ACCOUNT_BINDING_STATE_CONFLICT;
import static com.project.optrabidz.financial.application.error.FinancialErrors.RECEIVING_ACCOUNT_NOT_READY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FinancialPaymentAccountBindingErrorContractTest {
    private static final String PROTECTED_DIAGNOSTIC = "accountId=44 bindingId=91 key=secret-like-value";

    @ParameterizedTest
    @MethodSource("descriptors")
    void exposesApprovedBindingDescriptors(ErrorDescriptor descriptor,
                                           String code,
                                           ErrorCategory category,
                                           String publicMessage) {
        assertThat(descriptor).isEqualTo(new ErrorDescriptor(code, category, publicMessage));
    }

    @ParameterizedTest
    @MethodSource("failures")
    void typedBindingFailuresKeepDiagnosticsOutOfPublicMessages(
            Function<String, ApplicationException> factory,
            ErrorDescriptor descriptor,
            String diagnosticCode
    ) {
        ApplicationException failure = factory.apply(PROTECTED_DIAGNOSTIC);

        assertThat(failure.descriptor()).isSameAs(descriptor);
        assertThat(failure.diagnosticCode()).isEqualTo(diagnosticCode);
        assertThat(failure.getMessage()).contains(PROTECTED_DIAGNOSTIC);
        assertThat(failure.descriptor().publicMessage()).doesNotContain("accountId", "bindingId", "secret");
    }

    private static Stream<Arguments> descriptors() {
        return Stream.of(
                arguments(PAYMENT_ACCOUNT_BINDING_NOT_FOUND, "PAYMENT_ACCOUNT_BINDING_NOT_FOUND",
                        ErrorCategory.NOT_FOUND, "The requested payment account binding was not found"),
                arguments(PAYMENT_ACCOUNT_BINDING_ALREADY_EXISTS, "PAYMENT_ACCOUNT_BINDING_ALREADY_EXISTS",
                        ErrorCategory.CONFLICT, "An active payment account binding already exists"),
                arguments(PAYMENT_ACCOUNT_BINDING_STATE_CONFLICT, "PAYMENT_ACCOUNT_BINDING_STATE_CONFLICT",
                        ErrorCategory.CONFLICT, "The payment account binding state no longer permits this operation"),
                arguments(PAYMENT_ACCOUNT_BINDING_IDEMPOTENCY_CONFLICT,
                        "PAYMENT_ACCOUNT_BINDING_IDEMPOTENCY_CONFLICT", ErrorCategory.CONFLICT,
                        "The idempotency key was already used for a different binding command"),
                arguments(RECEIVING_ACCOUNT_NOT_READY, "RECEIVING_ACCOUNT_NOT_READY",
                        ErrorCategory.BUSINESS_RULE,
                        "A verified receiving account is required for this operation")
        );
    }

    private static Stream<Arguments> failures() {
        return Stream.of(
                arguments((Function<String, ApplicationException>) PaymentAccountBindingNotFoundException::new,
                        PAYMENT_ACCOUNT_BINDING_NOT_FOUND, "FINANCIAL.PAYMENT_ACCOUNT_BINDING.NOT_FOUND"),
                arguments((Function<String, ApplicationException>) PaymentAccountBindingAlreadyExistsException::new,
                        PAYMENT_ACCOUNT_BINDING_ALREADY_EXISTS, "FINANCIAL.PAYMENT_ACCOUNT_BINDING.ALREADY_EXISTS"),
                arguments((Function<String, ApplicationException>) PaymentAccountBindingStateConflictException::new,
                        PAYMENT_ACCOUNT_BINDING_STATE_CONFLICT, "FINANCIAL.PAYMENT_ACCOUNT_BINDING.STATE.CONFLICT"),
                arguments((Function<String, ApplicationException>) PaymentAccountBindingIdempotencyConflictException::new,
                        PAYMENT_ACCOUNT_BINDING_IDEMPOTENCY_CONFLICT,
                        "FINANCIAL.PAYMENT_ACCOUNT_BINDING.IDEMPOTENCY.CONFLICT")
        );
    }
}

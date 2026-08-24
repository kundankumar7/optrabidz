package com.project.optrabidz.financial.application;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.financial.application.exception.PaymentAlreadyConfirmedException;
import com.project.optrabidz.financial.application.exception.PaymentAttemptNotFoundException;
import com.project.optrabidz.financial.application.exception.PaymentIntentExpiredException;
import com.project.optrabidz.financial.application.exception.PaymentIntentNotActiveException;
import com.project.optrabidz.financial.application.exception.PaymentIntentNotFoundException;
import com.project.optrabidz.financial.application.exception.PaymentProviderMismatchException;
import com.project.optrabidz.financial.application.exception.PaymentStateConflictException;
import com.project.optrabidz.financial.application.exception.UnsupportedPaymentMethodException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_ALREADY_CONFIRMED;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_ATTEMPT_NOT_FOUND;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_INTENT_EXPIRED;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_INTENT_NOT_ACTIVE;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_INTENT_NOT_FOUND;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_METHOD_UNSUPPORTED;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_PROVIDER_MISMATCH;
import static com.project.optrabidz.financial.application.error.FinancialErrors.PAYMENT_STATE_CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FinancialPaymentErrorContractTest {
    private static final String PROTECTED_DIAGNOSTIC =
            "protected-provider-sentinel";

    @ParameterizedTest
    @MethodSource("descriptors")
    void exposesApprovedPaymentDescriptors(
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
    void typedPaymentFailuresKeepProtectedDiagnosticsOutOfPublicMessages(
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
                arguments(PAYMENT_INTENT_NOT_FOUND,
                        "PAYMENT_INTENT_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        "The requested payment intent was not found"),
                arguments(PAYMENT_ATTEMPT_NOT_FOUND,
                        "PAYMENT_ATTEMPT_NOT_FOUND",
                        ErrorCategory.NOT_FOUND,
                        "The requested payment attempt was not found"),
                arguments(PAYMENT_INTENT_EXPIRED,
                        "PAYMENT_INTENT_EXPIRED",
                        ErrorCategory.CONFLICT,
                        "The payment intent has expired"),
                arguments(PAYMENT_INTENT_NOT_ACTIVE,
                        "PAYMENT_INTENT_NOT_ACTIVE",
                        ErrorCategory.CONFLICT,
                        "The payment intent is not active"),
                arguments(PAYMENT_ALREADY_CONFIRMED,
                        "PAYMENT_ALREADY_CONFIRMED",
                        ErrorCategory.CONFLICT,
                        "The payment has already been confirmed"),
                arguments(PAYMENT_STATE_CONFLICT,
                        "PAYMENT_STATE_CONFLICT",
                        ErrorCategory.CONFLICT,
                        "The payment state no longer permits this operation"),
                arguments(PAYMENT_METHOD_UNSUPPORTED,
                        "PAYMENT_METHOD_UNSUPPORTED",
                        ErrorCategory.BUSINESS_RULE,
                        "The selected payment method is not supported"),
                arguments(PAYMENT_PROVIDER_MISMATCH,
                        "PAYMENT_PROVIDER_MISMATCH",
                        ErrorCategory.BUSINESS_RULE,
                        "The payment attempt cannot be handled by this provider")
        );
    }

    private static Stream<Arguments> failures() {
        return Stream.of(
                arguments(
                        (Function<String, ApplicationException>)
                                PaymentIntentNotFoundException::new,
                        PAYMENT_INTENT_NOT_FOUND,
                        "FINANCIAL.PAYMENT.INTENT.NOT_FOUND"),
                arguments(
                        (Function<String, ApplicationException>)
                                PaymentAttemptNotFoundException::new,
                        PAYMENT_ATTEMPT_NOT_FOUND,
                        "FINANCIAL.PAYMENT.ATTEMPT.NOT_FOUND"),
                arguments(
                        (Function<String, ApplicationException>)
                                PaymentIntentExpiredException::new,
                        PAYMENT_INTENT_EXPIRED,
                        "FINANCIAL.PAYMENT.INTENT.EXPIRED"),
                arguments(
                        (Function<String, ApplicationException>)
                                PaymentIntentNotActiveException::new,
                        PAYMENT_INTENT_NOT_ACTIVE,
                        "FINANCIAL.PAYMENT.INTENT.NOT.ACTIVE"),
                arguments(
                        (Function<String, ApplicationException>)
                                PaymentAlreadyConfirmedException::new,
                        PAYMENT_ALREADY_CONFIRMED,
                        "FINANCIAL.PAYMENT.ALREADY.CONFIRMED"),
                arguments(
                        (Function<String, ApplicationException>)
                                PaymentStateConflictException::new,
                        PAYMENT_STATE_CONFLICT,
                        "FINANCIAL.PAYMENT.STATE.CONFLICT"),
                arguments(
                        (Function<String, ApplicationException>)
                                UnsupportedPaymentMethodException::new,
                        PAYMENT_METHOD_UNSUPPORTED,
                        "FINANCIAL.PAYMENT.METHOD.UNSUPPORTED"),
                arguments(
                        (Function<String, ApplicationException>)
                                PaymentProviderMismatchException::new,
                        PAYMENT_PROVIDER_MISMATCH,
                        "FINANCIAL.PAYMENT.PROVIDER.MISMATCH")
        );
    }
}

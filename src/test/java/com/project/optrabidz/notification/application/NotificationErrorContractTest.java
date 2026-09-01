package com.project.optrabidz.notification.application;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.notification.application.exception.NotificationNotFoundException;
import com.project.optrabidz.notification.application.exception.NotificationSubscriptionNotFoundException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.project.optrabidz.notification.application.error.NotificationErrors.NOTIFICATION_NOT_FOUND;
import static com.project.optrabidz.notification.application.error.NotificationErrors.NOTIFICATION_SUBSCRIPTION_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NotificationErrorContractTest {
    private static final String PROTECTED_DIAGNOSTIC =
            "accountId=901 recipientId=902 subscriptionId=903 endpoint=secret";

    @ParameterizedTest
    @MethodSource("descriptors")
    void exposesApprovedPublicDescriptors(
            ErrorDescriptor descriptor,
            String code,
            String publicMessage
    ) {
        assertThat(descriptor).isEqualTo(new ErrorDescriptor(
                code,
                ErrorCategory.NOT_FOUND,
                publicMessage
        ));
    }

    private static Stream<Arguments> descriptors() {
        return Stream.of(
                arguments(
                        NOTIFICATION_NOT_FOUND,
                        "NOTIFICATION_NOT_FOUND",
                        "The requested notification was not found"
                ),
                arguments(
                        NOTIFICATION_SUBSCRIPTION_NOT_FOUND,
                        "NOTIFICATION_SUBSCRIPTION_NOT_FOUND",
                        "The requested notification subscription was not found"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("failures")
    void typedFailuresKeepProtectedDiagnosticsOutOfPublicMessages(
            Function<String, ApplicationException> factory,
            ErrorDescriptor descriptor,
            String diagnosticCode
    ) {
        ApplicationException failure = factory.apply(PROTECTED_DIAGNOSTIC);

        assertThat(failure.descriptor()).isSameAs(descriptor);
        assertThat(failure.diagnosticCode()).isEqualTo(diagnosticCode);
        assertThat(failure.getMessage()).contains(PROTECTED_DIAGNOSTIC);
        assertThat(failure.descriptor().publicMessage())
                .doesNotContain(
                        "accountId=901",
                        "recipientId=902",
                        "subscriptionId=903",
                        "endpoint=secret"
                );
    }

    private static Stream<Arguments> failures() {
        return Stream.of(
                arguments(
                        (Function<String, ApplicationException>)
                                NotificationNotFoundException::new,
                        NOTIFICATION_NOT_FOUND,
                        "NOTIFICATION.RECIPIENT.NOT_FOUND"
                ),
                arguments(
                        (Function<String, ApplicationException>)
                                NotificationSubscriptionNotFoundException::new,
                        NOTIFICATION_SUBSCRIPTION_NOT_FOUND,
                        "NOTIFICATION.SUBSCRIPTION.NOT_FOUND"
                )
        );
    }
}

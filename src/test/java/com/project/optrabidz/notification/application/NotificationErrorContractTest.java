package com.project.optrabidz.notification.application;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.project.optrabidz.notification.application.error.NotificationErrors.NOTIFICATION_NOT_FOUND;
import static com.project.optrabidz.notification.application.error.NotificationErrors.NOTIFICATION_SUBSCRIPTION_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NotificationErrorContractTest {
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
}

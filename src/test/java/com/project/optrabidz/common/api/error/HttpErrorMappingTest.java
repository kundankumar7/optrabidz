package com.project.optrabidz.common.api.error;

import com.project.optrabidz.common.error.ErrorCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class HttpErrorMappingTest {
    @ParameterizedTest
    @MethodSource("categoryMappings")
    void mapsEveryNeutralCategory(
            ErrorCategory category,
            HttpStatus status,
            String title
    ) {
        HttpErrorMapping mapping = HttpErrorMapping.forCategory(category);

        assertThat(mapping.status()).isEqualTo(status);
        assertThat(mapping.title()).isEqualTo(title);
    }

    @Test
    void rejectsMissingCategory() {
        assertThatNullPointerException()
                .isThrownBy(() -> HttpErrorMapping.forCategory(null));
    }

    private static Stream<Arguments> categoryMappings() {
        return Stream.of(
                Arguments.of(ErrorCategory.VALIDATION,
                        HttpStatus.BAD_REQUEST, "Request validation failed"),
                Arguments.of(ErrorCategory.AUTHENTICATION,
                        HttpStatus.UNAUTHORIZED, "Authentication required"),
                Arguments.of(ErrorCategory.AUTHORIZATION,
                        HttpStatus.FORBIDDEN, "Access denied"),
                Arguments.of(ErrorCategory.NOT_FOUND,
                        HttpStatus.NOT_FOUND, "Resource not found"),
                Arguments.of(ErrorCategory.CONFLICT,
                        HttpStatus.CONFLICT, "Request conflict"),
                Arguments.of(ErrorCategory.BUSINESS_RULE,
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Business rule violation"),
                Arguments.of(ErrorCategory.INTERNAL,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal server error")
        );
    }
}

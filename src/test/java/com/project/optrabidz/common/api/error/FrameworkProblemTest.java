package com.project.optrabidz.common.api.error;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FrameworkProblemTest {
    @ParameterizedTest
    @MethodSource("frameworkProblems")
    void definesStableFrameworkProblem(
            FrameworkProblem problem,
            HttpStatus status,
            String title,
            String detail
    ) {
        assertThat(problem.code()).isEqualTo(problem.name());
        assertThat(problem.mapping().status()).isEqualTo(status);
        assertThat(problem.mapping().title()).isEqualTo(title);
        assertThat(problem.detail()).isEqualTo(detail);
    }

    private static Stream<Arguments> frameworkProblems() {
        assertThat(FrameworkProblem.values()).containsExactly(
                FrameworkProblem.VALIDATION_ERROR,
                FrameworkProblem.MALFORMED_REQUEST,
                FrameworkProblem.ENDPOINT_NOT_FOUND,
                FrameworkProblem.METHOD_NOT_ALLOWED,
                FrameworkProblem.NOT_ACCEPTABLE,
                FrameworkProblem.UNSUPPORTED_MEDIA_TYPE
        );

        return Stream.of(
                Arguments.of(
                        FrameworkProblem.VALIDATION_ERROR,
                        HttpStatus.BAD_REQUEST,
                        "Request validation failed",
                        "One or more request values are invalid"
                ),
                Arguments.of(
                        FrameworkProblem.MALFORMED_REQUEST,
                        HttpStatus.BAD_REQUEST,
                        "Malformed request",
                        "The request body is malformed"
                ),
                Arguments.of(
                        FrameworkProblem.ENDPOINT_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Endpoint not found",
                        "The requested endpoint is unavailable"
                ),
                Arguments.of(
                        FrameworkProblem.METHOD_NOT_ALLOWED,
                        HttpStatus.METHOD_NOT_ALLOWED,
                        "Method not allowed",
                        "The HTTP method is not supported for this endpoint"
                ),
                Arguments.of(
                        FrameworkProblem.NOT_ACCEPTABLE,
                        HttpStatus.NOT_ACCEPTABLE,
                        "Response type not acceptable",
                        "The requested response media type is not available"
                ),
                Arguments.of(
                        FrameworkProblem.UNSUPPORTED_MEDIA_TYPE,
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Unsupported media type",
                        "The request media type is not supported"
                )
        );
    }
}

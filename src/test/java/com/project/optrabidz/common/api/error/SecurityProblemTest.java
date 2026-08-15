package com.project.optrabidz.common.api.error;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityProblemTest {
    @ParameterizedTest
    @MethodSource("securityProblems")
    void definesStableSecurityProblem(
            SecurityProblem problem,
            HttpStatus status,
            String title,
            String detail
    ) {
        assertThat(problem.code()).isEqualTo(problem.name());
        assertThat(problem.mapping().status()).isEqualTo(status);
        assertThat(problem.mapping().title()).isEqualTo(title);
        assertThat(problem.detail()).isEqualTo(detail);
    }

    private static Stream<Arguments> securityProblems() {
        assertThat(SecurityProblem.values()).containsExactly(
                SecurityProblem.AUTHENTICATION_REQUIRED,
                SecurityProblem.AUTHORIZATION_FAILED,
                SecurityProblem.CSRF_VALIDATION_FAILED
        );

        return Stream.of(
                Arguments.of(
                        SecurityProblem.AUTHENTICATION_REQUIRED,
                        HttpStatus.UNAUTHORIZED,
                        "Authentication required",
                        "Authentication is required"
                ),
                Arguments.of(
                        SecurityProblem.AUTHORIZATION_FAILED,
                        HttpStatus.FORBIDDEN,
                        "Access denied",
                        "You are not authorized to perform this action"
                ),
                Arguments.of(
                        SecurityProblem.CSRF_VALIDATION_FAILED,
                        HttpStatus.FORBIDDEN,
                        "Request security validation failed",
                        "Request security validation failed"
                )
        );
    }
}

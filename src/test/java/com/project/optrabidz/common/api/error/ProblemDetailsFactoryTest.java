package com.project.optrabidz.common.api.error;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsFactoryTest {
    private static final Instant NOW =
            Instant.parse("2026-08-15T04:00:00Z");
    private static final ErrorDescriptor DESCRIPTOR = new ErrorDescriptor(
            "LISTING_NOT_FOUND",
            ErrorCategory.NOT_FOUND,
            "The requested funding listing is unavailable"
    );

    private final ProblemDetailsFactory factory = new ProblemDetailsFactory(
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void createsTheApprovedPublicContract() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/listings/42"
        );
        request.addHeader("X-Request-Id", "request-123");
        ApplicationException exception = new ApplicationException(
                DESCRIPTOR,
                "MARKETPLACE.LISTING_LOOKUP_FAILED",
                "Database row 42 was absent"
        );

        ProblemDetail problem = factory.create(
                exception,
                HttpErrorMapping.forCategory(ErrorCategory.NOT_FOUND),
                request
        );

        assertThat(problem.getType()).isEqualTo(
                URI.create("urn:optrabidz:problem:listing-not-found")
        );
        assertThat(problem.getTitle()).isEqualTo("Resource not found");
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).isEqualTo(
                "The requested funding listing is unavailable"
        );
        assertThat(problem.getInstance()).isEqualTo(
                URI.create("urn:optrabidz:request:request-123")
        );
        assertThat(problem.getProperties())
                .containsOnlyKeys("code", "requestId", "timestamp")
                .containsEntry("code", "LISTING_NOT_FOUND")
                .containsEntry("requestId", "request-123")
                .containsEntry("timestamp", NOW);
    }

    @Test
    void neverCopiesProtectedDiagnosticsIntoTheProblem() {
        String secret = "password=hunter2 table=credential";
        MockHttpServletRequest request = new MockHttpServletRequest();
        ApplicationException exception = new ApplicationException(
                DESCRIPTOR,
                "DATABASE.POSTGRES_CONSTRAINT",
                secret,
                new IllegalStateException("jdbc:postgresql://internal-host")
        );

        ProblemDetail problem = factory.create(
                exception,
                HttpErrorMapping.forCategory(ErrorCategory.NOT_FOUND),
                request
        );

        assertThat(problem.toString())
                .doesNotContain(secret)
                .doesNotContain(exception.diagnosticCode())
                .doesNotContain("internal-host")
                .doesNotContain("IllegalStateException");
    }
}

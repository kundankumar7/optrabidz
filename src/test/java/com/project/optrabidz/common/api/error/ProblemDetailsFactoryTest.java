package com.project.optrabidz.common.api.error;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
                .containsEntry("timestamp", NOW.toString());
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

    @Test
    void createsFrameworkProblemWithSafeViolations() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "request-123");

        ProblemDetail problem = factory.createFramework(
                FrameworkProblem.VALIDATION_ERROR,
                List.of(new ValidationViolation(
                        "items[0].amount",
                        "must be greater than zero"
                )),
                request
        );

        assertThat(problem.getType()).isEqualTo(
                URI.create("urn:optrabidz:problem:validation-error")
        );
        assertThat(problem.getTitle()).isEqualTo(
                "Request validation failed"
        );
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo(
                "One or more request values are invalid"
        );
        assertThat(problem.getProperties())
                .containsEntry("code", "VALIDATION_ERROR")
                .containsEntry("requestId", "request-123")
                .containsEntry("timestamp", NOW.toString())
                .containsEntry("violations", List.of(
                        new ValidationViolation(
                                "items[0].amount",
                                "must be greater than zero"
                        )
                ));
    }

    @Test
    void omitsViolationsWhenFrameworkFailureHasNone() {
        ProblemDetail problem = factory.createFramework(
                FrameworkProblem.MALFORMED_REQUEST,
                List.of(),
                new MockHttpServletRequest()
        );

        assertThat(problem.getProperties())
                .doesNotContainKey("violations");
    }

    @Test
    void doesNotCopyUnrelatedRequestDataIntoFrameworkProblem() {
        String secret = "password=hunter2";
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/test/" + secret
        );
        request.addHeader("X-Debug", secret);

        ProblemDetail problem = factory.createFramework(
                FrameworkProblem.MALFORMED_REQUEST,
                List.of(),
                request
        );

        assertThat(problem.toString()).doesNotContain(secret);
    }

    @Test
    void createsSecurityProblemFromTheAllowlistedCatalogue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "request-123");

        ProblemDetail problem = factory.createSecurity(
                SecurityProblem.CSRF_VALIDATION_FAILED,
                request
        );

        assertThat(problem.getType()).isEqualTo(
                URI.create("urn:optrabidz:problem:csrf-validation-failed")
        );
        assertThat(problem.getTitle()).isEqualTo(
                "Request security validation failed"
        );
        assertThat(problem.getStatus()).isEqualTo(403);
        assertThat(problem.getDetail()).isEqualTo(
                "Request security validation failed"
        );
        assertThat(problem.getInstance()).isEqualTo(
                URI.create("urn:optrabidz:request:request-123")
        );
        assertThat(problem.getProperties())
                .containsOnlyKeys("code", "requestId", "timestamp")
                .containsEntry("code", "CSRF_VALIDATION_FAILED")
                .containsEntry("requestId", "request-123")
                .containsEntry("timestamp", NOW.toString())
                .doesNotContainKey("violations");
    }

    @Test
    void securityProblemDoesNotCopyUnrelatedRequestData() {
        String secret = "csrf-secret-value";
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/test/" + secret
        );
        request.addHeader("X-Request-Id", "request-123");
        request.addHeader("Authorization", "Bearer " + secret);
        request.setCookies(new MockCookie("XSRF-TOKEN", secret));
        request.setAttribute("securityException", secret);

        ProblemDetail problem = factory.createSecurity(
                SecurityProblem.AUTHORIZATION_FAILED,
                request
        );

        assertThat(problem.toString())
                .doesNotContain(secret)
                .doesNotContain("Authorization")
                .doesNotContain("securityException");
    }

    @Test
    void rejectsBlankValidationViolationParts() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ValidationViolation(" ", "is invalid"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ValidationViolation("field", " "));
    }
}

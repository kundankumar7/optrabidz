package com.project.optrabidz.common.api.error;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.common.observability.RequestIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public final class ProblemDetailsFactory {
    private final Clock clock;

    public ProblemDetailsFactory() {
        this(Clock.systemUTC());
    }

    ProblemDetailsFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ProblemDetail create(
            ApplicationException exception,
            HttpErrorMapping mapping,
            HttpServletRequest request
    ) {
        Objects.requireNonNull(exception, "exception must not be null");
        Objects.requireNonNull(mapping, "mapping must not be null");

        ErrorDescriptor descriptor = exception.descriptor();
        return createProblem(
                descriptor.code(),
                descriptor.publicMessage(),
                mapping,
                List.of(),
                request
        );
    }

    ProblemDetail createFramework(
            FrameworkProblem frameworkProblem,
            List<ValidationViolation> violations,
            HttpServletRequest request
    ) {
        Objects.requireNonNull(
                frameworkProblem,
                "frameworkProblem must not be null"
        );
        Objects.requireNonNull(violations, "violations must not be null");
        return createProblem(
                frameworkProblem.code(),
                frameworkProblem.detail(),
                frameworkProblem.mapping(),
                List.copyOf(violations),
                request
        );
    }

    private ProblemDetail createProblem(
            String code,
            String detail,
            HttpErrorMapping mapping,
            List<ValidationViolation> violations,
            HttpServletRequest request
    ) {
        Objects.requireNonNull(request, "request must not be null");
        String requestId = RequestIdProvider.resolveOrCreate(request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                mapping.status(),
                detail
        );

        problem.setType(URI.create(
                "urn:optrabidz:problem:" + toProblemSlug(code)
        ));
        problem.setTitle(mapping.title());
        problem.setInstance(URI.create(
                "urn:optrabidz:request:" + requestId
        ));
        problem.setProperty("code", code);
        problem.setProperty("requestId", requestId);
        problem.setProperty("timestamp", clock.instant().toString());
        if (!violations.isEmpty()) {
            problem.setProperty("violations", violations);
        }
        return problem;
    }

    private String toProblemSlug(String code) {
        return code.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}

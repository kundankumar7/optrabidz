package com.project.optrabidz.common.api.error;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.common.observability.RequestIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Clock;
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
        String requestId = RequestIdProvider.resolveOrCreate(request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                mapping.status(),
                descriptor.publicMessage()
        );

        problem.setType(URI.create(
                "urn:optrabidz:problem:" + toProblemSlug(descriptor.code())
        ));
        problem.setTitle(mapping.title());
        problem.setInstance(URI.create(
                "urn:optrabidz:request:" + requestId
        ));
        problem.setProperty("code", descriptor.code());
        problem.setProperty("requestId", requestId);
        problem.setProperty("timestamp", clock.instant().toString());
        return problem;
    }

    private String toProblemSlug(String code) {
        return code.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}

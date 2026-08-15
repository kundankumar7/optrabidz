package com.project.optrabidz.common.api.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
public final class SecurityProblemResponseWriter {
    private final ProblemDetailsFactory problemDetailsFactory;
    private final ObjectMapper objectMapper;

    public SecurityProblemResponseWriter(
            ProblemDetailsFactory problemDetailsFactory,
            ObjectMapper objectMapper
    ) {
        this.problemDetailsFactory = Objects.requireNonNull(
                problemDetailsFactory,
                "problemDetailsFactory"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    public void write(
            SecurityProblem problem,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Objects.requireNonNull(problem, "problem");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(response, "response");

        ProblemDetail body = problemDetailsFactory.createSecurity(
                problem,
                request
        );
        response.setStatus(problem.mapping().status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

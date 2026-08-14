package com.project.optrabidz.common.api.error;

import com.project.optrabidz.common.error.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class RestExceptionHandler {
    private final ProblemDetailsFactory problemDetailsFactory;

    public RestExceptionHandler(
            ProblemDetailsFactory problemDetailsFactory
    ) {
        this.problemDetailsFactory = Objects.requireNonNull(
                problemDetailsFactory,
                "problemDetailsFactory must not be null"
        );
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ProblemDetail> handleApplicationException(
            ApplicationException exception,
            HttpServletRequest request
    ) {
        HttpErrorMapping mapping = HttpErrorMapping.forCategory(
                exception.descriptor().category()
        );
        ProblemDetail problem = problemDetailsFactory.create(
                exception,
                mapping,
                request
        );

        return ResponseEntity.status(mapping.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}

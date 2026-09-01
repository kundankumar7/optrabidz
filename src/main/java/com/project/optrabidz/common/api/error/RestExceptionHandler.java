package com.project.optrabidz.common.api.error;

import com.project.optrabidz.common.error.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Objects;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class RestExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(
            RestExceptionHandler.class
    );

    private final ProblemDetailsFactory problemDetailsFactory;
    private final ValidationViolationMapper validationViolationMapper;

    public RestExceptionHandler(
            ProblemDetailsFactory problemDetailsFactory,
            ValidationViolationMapper validationViolationMapper
    ) {
        this.problemDetailsFactory = Objects.requireNonNull(
                problemDetailsFactory,
                "problemDetailsFactory must not be null"
        );
        this.validationViolationMapper = Objects.requireNonNull(
                validationViolationMapper,
                "validationViolationMapper must not be null"
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled MVC exception", exception);
        return frameworkResponse(
                FrameworkProblem.INTERNAL_SERVER_ERROR,
                List.of(),
                new HttpHeaders(),
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        return frameworkResponse(
                FrameworkProblem.VALIDATION_ERROR,
                validationViolationMapper.fromConstraintViolations(
                        exception.getConstraintViolations()
                ),
                new HttpHeaders(),
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkResponse(
                FrameworkProblem.VALIDATION_ERROR,
                validationViolationMapper.fromBindingResult(
                        exception.getBindingResult()
                ),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkResponse(
                FrameworkProblem.VALIDATION_ERROR,
                validationViolationMapper.fromMethodValidation(exception),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkResponse(
                FrameworkProblem.VALIDATION_ERROR,
                List.of(validationViolationMapper.missing(
                        exception.getParameterName()
                )),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(
            MissingServletRequestPartException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkResponse(
                FrameworkProblem.VALIDATION_ERROR,
                List.of(validationViolationMapper.missing(
                        exception.getRequestPartName()
                )),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            ServletRequestBindingException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String field = exception instanceof MissingRequestHeaderException missing
                ? missing.getHeaderName()
                : null;
        return frameworkResponse(
                FrameworkProblem.VALIDATION_ERROR,
                List.of(validationViolationMapper.missing(field)),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String field = exception instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : null;
        return frameworkResponse(
                FrameworkProblem.VALIDATION_ERROR,
                List.of(validationViolationMapper.typeMismatch(field)),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkResponse(
                FrameworkProblem.MALFORMED_REQUEST,
                List.of(),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkResponse(
                FrameworkProblem.ENDPOINT_NOT_FOUND,
                List.of(),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkResponse(
                FrameworkProblem.ENDPOINT_NOT_FOUND,
                List.of(),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkResponse(
                FrameworkProblem.METHOD_NOT_ALLOWED,
                List.of(),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkResponse(
                FrameworkProblem.NOT_ACCEPTABLE,
                List.of(),
                headers,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkResponse(
                FrameworkProblem.UNSUPPORTED_MEDIA_TYPE,
                List.of(),
                headers,
                request
        );
    }

    private ResponseEntity<Object> frameworkResponse(
            FrameworkProblem problem,
            List<ValidationViolation> violations,
            HttpHeaders headers,
            WebRequest request
    ) {
        HttpServletRequest servletRequest =
                ((ServletWebRequest) request).getRequest();
        return frameworkResponse(
                problem,
                violations,
                headers,
                servletRequest
        );
    }

    private ResponseEntity<Object> frameworkResponse(
            FrameworkProblem problem,
            List<ValidationViolation> violations,
            HttpHeaders headers,
            HttpServletRequest request
    ) {
        ProblemDetail body = problemDetailsFactory.createFramework(
                problem,
                violations,
                request
        );
        return ResponseEntity.status(problem.mapping().status())
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}

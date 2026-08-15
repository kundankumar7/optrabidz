package com.project.optrabidz.common.api.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Component
public final class ValidationViolationMapper {
    private static final String REQUEST_FIELD = "_request";
    private static final String INVALID_MESSAGE = "is invalid";

    private static final Map<String, String> SAFE_MESSAGES = Map.ofEntries(
            Map.entry("NotNull", "must not be null"),
            Map.entry("NotBlank", "must not be blank"),
            Map.entry("NotEmpty", "must not be empty"),
            Map.entry("Email", "must be a well-formed email address"),
            Map.entry("Size", "size is outside the allowed range"),
            Map.entry("Min", "must be at least the minimum allowed value"),
            Map.entry("DecimalMin", "must be at least the minimum allowed value"),
            Map.entry("Max", "must not exceed the maximum allowed value"),
            Map.entry("DecimalMax", "must not exceed the maximum allowed value"),
            Map.entry("Positive", "must be greater than zero"),
            Map.entry("PositiveOrZero", "must be zero or greater"),
            Map.entry("Negative", "must be less than zero"),
            Map.entry("NegativeOrZero", "must be zero or less"),
            Map.entry("Pattern", "has an invalid format"),
            Map.entry("Past", "must be in the past"),
            Map.entry("PastOrPresent", "must be in the past or present"),
            Map.entry("Future", "must be in the future"),
            Map.entry("FutureOrPresent", "must be in the present or future")
    );

    List<ValidationViolation> fromBindingResult(BindingResult result) {
        Objects.requireNonNull(result, "result must not be null");
        Stream<ValidationViolation> fieldViolations = result.getFieldErrors()
                .stream()
                .map(error -> new ValidationViolation(
                        safeField(error.getField()),
                        safeMessage(error.getCodes())
                ));
        Stream<ValidationViolation> requestViolations = result
                .getGlobalErrors()
                .stream()
                .map(error -> new ValidationViolation(
                        REQUEST_FIELD,
                        safeMessage(error.getCodes())
                ));
        return sortedDistinct(Stream.concat(
                fieldViolations,
                requestViolations
        ));
    }

    List<ValidationViolation> fromMethodValidation(
            HandlerMethodValidationException exception
    ) {
        Objects.requireNonNull(exception, "exception must not be null");
        return sortedDistinct(exception.getAllValidationResults()
                .stream()
                .flatMap(this::toMethodViolations));
    }

    List<ValidationViolation> fromConstraintViolations(
            Set<? extends ConstraintViolation<?>> violations
    ) {
        Objects.requireNonNull(violations, "violations must not be null");
        return sortedDistinct(violations.stream()
                .map(violation -> new ValidationViolation(
                        safeConstraintPath(violation.getPropertyPath()),
                        safeConstraintMessage(violation)
                )));
    }

    ValidationViolation missing(String field) {
        return new ValidationViolation(safeField(field), "is required");
    }

    ValidationViolation typeMismatch(String field) {
        return new ValidationViolation(
                safeField(field),
                "has an invalid type"
        );
    }

    private Stream<ValidationViolation> toMethodViolations(
            ParameterValidationResult result
    ) {
        String field = parameterField(result.getMethodParameter());
        return result.getResolvableErrors()
                .stream()
                .map(error -> new ValidationViolation(
                        field,
                        safeMessage(error)
                ));
    }

    private String parameterField(MethodParameter parameter) {
        String declaredName = annotationName(parameter);
        if (declaredName != null) {
            return safeField(declaredName);
        }
        String parameterName = parameter.getParameterName();
        if (parameterName != null) {
            return safeField(parameterName);
        }
        return "arg" + parameter.getParameterIndex();
    }

    private String annotationName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(
                RequestParam.class
        );
        if (requestParam != null) {
            return firstPresent(requestParam.name(), requestParam.value());
        }
        RequestHeader requestHeader = parameter.getParameterAnnotation(
                RequestHeader.class
        );
        if (requestHeader != null) {
            return firstPresent(
                    requestHeader.name(),
                    requestHeader.value()
            );
        }
        PathVariable pathVariable = parameter.getParameterAnnotation(
                PathVariable.class
        );
        if (pathVariable != null) {
            return firstPresent(pathVariable.name(), pathVariable.value());
        }
        CookieValue cookieValue = parameter.getParameterAnnotation(
                CookieValue.class
        );
        if (cookieValue != null) {
            return firstPresent(cookieValue.name(), cookieValue.value());
        }
        RequestPart requestPart = parameter.getParameterAnnotation(
                RequestPart.class
        );
        if (requestPart != null) {
            return firstPresent(requestPart.name(), requestPart.value());
        }
        return null;
    }

    private String firstPresent(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private String safeConstraintPath(Path path) {
        StringBuilder field = new StringBuilder();
        for (Path.Node node : path) {
            ElementKind kind = node.getKind();
            if (kind == ElementKind.PROPERTY
                    || kind == ElementKind.PARAMETER) {
                appendSegment(field, node.getName());
                appendIndex(field, node.getIndex());
            } else if (kind == ElementKind.CONTAINER_ELEMENT) {
                appendIndex(field, node.getIndex());
            }
        }
        return safeField(field.toString());
    }

    private void appendSegment(StringBuilder field, String segment) {
        if (segment == null || segment.isBlank()) {
            return;
        }
        if (!field.isEmpty()) {
            field.append('.');
        }
        field.append(segment);
    }

    private void appendIndex(StringBuilder field, Integer index) {
        if (index != null) {
            field.append('[').append(index).append(']');
        }
    }

    private String safeConstraintMessage(
            ConstraintViolation<?> violation
    ) {
        String constraintName = violation.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();
        return SAFE_MESSAGES.getOrDefault(
                constraintName,
                INVALID_MESSAGE
        );
    }

    private String safeMessage(MessageSourceResolvable error) {
        return safeMessage(error.getCodes());
    }

    private String safeMessage(String[] codes) {
        if (codes == null) {
            return INVALID_MESSAGE;
        }
        return SAFE_MESSAGES.entrySet()
                .stream()
                .filter(entry -> Arrays.stream(codes).anyMatch(code ->
                        code.equals(entry.getKey())
                                || code.startsWith(entry.getKey() + ".")
                ))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(INVALID_MESSAGE);
    }

    private String safeField(String field) {
        if (field == null || field.isBlank()) {
            return REQUEST_FIELD;
        }
        return field.strip();
    }

    private List<ValidationViolation> sortedDistinct(
            Stream<ValidationViolation> violations
    ) {
        return violations
                .distinct()
                .sorted(Comparator
                        .comparing(ValidationViolation::field)
                        .thenComparing(ValidationViolation::message))
                .toList();
    }
}

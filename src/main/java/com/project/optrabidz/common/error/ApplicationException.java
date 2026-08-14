package com.project.optrabidz.common.error;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class ApplicationException extends RuntimeException {
    private static final Pattern DIAGNOSTIC_CODE =
            Pattern.compile("[A-Z][A-Z0-9_.-]*");

    private final ErrorDescriptor descriptor;
    private final String diagnosticCode;
    private final List<ErrorDetail> details;

    public ApplicationException(
            ErrorDescriptor descriptor,
            String diagnosticCode,
            String diagnosticMessage
    ) {
        this(descriptor, diagnosticCode, diagnosticMessage, List.of(), null);
    }

    public ApplicationException(
            ErrorDescriptor descriptor,
            String diagnosticCode,
            String diagnosticMessage,
            Throwable cause
    ) {
        this(descriptor, diagnosticCode, diagnosticMessage, List.of(), cause);
    }

    public ApplicationException(
            ErrorDescriptor descriptor,
            String diagnosticCode,
            String diagnosticMessage,
            List<ErrorDetail> details
    ) {
        this(descriptor, diagnosticCode, diagnosticMessage, details, null);
    }

    public ApplicationException(
            ErrorDescriptor descriptor,
            String diagnosticCode,
            String diagnosticMessage,
            List<ErrorDetail> details,
            Throwable cause
    ) {
        super(requireDiagnosticMessage(diagnosticMessage), cause);
        this.descriptor = Objects.requireNonNull(
                descriptor,
                "descriptor must not be null"
        );
        if (diagnosticCode == null
                || !DIAGNOSTIC_CODE.matcher(diagnosticCode).matches()) {
            throw new IllegalArgumentException(
                    "diagnosticCode must use uppercase segments"
            );
        }
        this.diagnosticCode = diagnosticCode;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ErrorDescriptor descriptor() {
        return descriptor;
    }

    public String diagnosticCode() {
        return diagnosticCode;
    }

    public List<ErrorDetail> details() {
        return details;
    }

    private static String requireDiagnosticMessage(String diagnosticMessage) {
        if (diagnosticMessage == null || diagnosticMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "diagnosticMessage must not be blank"
            );
        }
        return diagnosticMessage.strip();
    }
}

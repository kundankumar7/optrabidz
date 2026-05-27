package com.project.optrabidz.common.api.exception;

import java.util.List;

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final List<ErrorField> fields;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of(), null);
    }

    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, List.of(), cause);
    }

    public ApiException(ErrorCode errorCode, String message, List<ErrorField> fields) {
        this(errorCode, message, fields, null);
    }

    public ApiException(ErrorCode errorCode, String message, List<ErrorField> fields, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ErrorField> getFields() {
        return fields;
    }
}

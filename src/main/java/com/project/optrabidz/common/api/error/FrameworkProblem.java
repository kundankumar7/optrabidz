package com.project.optrabidz.common.api.error;

import org.springframework.http.HttpStatus;

enum FrameworkProblem {
    VALIDATION_ERROR(
            new HttpErrorMapping(
                    HttpStatus.BAD_REQUEST,
                    "Request validation failed"
            ),
            "One or more request values are invalid"
    ),
    MALFORMED_REQUEST(
            new HttpErrorMapping(
                    HttpStatus.BAD_REQUEST,
                    "Malformed request"
            ),
            "The request body is malformed"
    ),
    ENDPOINT_NOT_FOUND(
            new HttpErrorMapping(
                    HttpStatus.NOT_FOUND,
                    "Endpoint not found"
            ),
            "The requested endpoint is unavailable"
    ),
    METHOD_NOT_ALLOWED(
            new HttpErrorMapping(
                    HttpStatus.METHOD_NOT_ALLOWED,
                    "Method not allowed"
            ),
            "The HTTP method is not supported for this endpoint"
    ),
    NOT_ACCEPTABLE(
            new HttpErrorMapping(
                    HttpStatus.NOT_ACCEPTABLE,
                    "Response type not acceptable"
            ),
            "The requested response media type is not available"
    ),
    UNSUPPORTED_MEDIA_TYPE(
            new HttpErrorMapping(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported media type"
            ),
            "The request media type is not supported"
    );

    private final HttpErrorMapping mapping;
    private final String detail;

    FrameworkProblem(HttpErrorMapping mapping, String detail) {
        this.mapping = mapping;
        this.detail = detail;
    }

    String code() {
        return name();
    }

    HttpErrorMapping mapping() {
        return mapping;
    }

    String detail() {
        return detail;
    }
}

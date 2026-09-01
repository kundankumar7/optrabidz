package com.project.optrabidz.common.api.error;

import org.springframework.http.HttpStatus;

public enum SecurityProblem {
    AUTHENTICATION_REQUIRED(
            new HttpErrorMapping(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            ),
            "Authentication is required"
    ),
    AUTHORIZATION_FAILED(
            new HttpErrorMapping(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            ),
            "You are not authorized to perform this action"
    ),
    CSRF_VALIDATION_FAILED(
            new HttpErrorMapping(
                    HttpStatus.FORBIDDEN,
                    "Request security validation failed"
            ),
            "Request security validation failed"
    );

    private final HttpErrorMapping mapping;
    private final String detail;

    SecurityProblem(HttpErrorMapping mapping, String detail) {
        this.mapping = mapping;
        this.detail = detail;
    }

    public String code() {
        return name();
    }

    public HttpErrorMapping mapping() {
        return mapping;
    }

    public String detail() {
        return detail;
    }
}

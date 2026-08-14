package com.project.optrabidz.common.api.error;

import com.project.optrabidz.common.error.ErrorCategory;
import org.springframework.http.HttpStatus;

import java.util.Objects;

public record HttpErrorMapping(HttpStatus status, String title) {
    public HttpErrorMapping {
        Objects.requireNonNull(status, "status must not be null");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        title = title.strip();
    }

    public static HttpErrorMapping forCategory(ErrorCategory category) {
        return switch (Objects.requireNonNull(
                category,
                "category must not be null"
        )) {
            case VALIDATION -> new HttpErrorMapping(
                    HttpStatus.BAD_REQUEST,
                    "Request validation failed"
            );
            case AUTHENTICATION -> new HttpErrorMapping(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
            case AUTHORIZATION -> new HttpErrorMapping(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
            case NOT_FOUND -> new HttpErrorMapping(
                    HttpStatus.NOT_FOUND,
                    "Resource not found"
            );
            case CONFLICT -> new HttpErrorMapping(
                    HttpStatus.CONFLICT,
                    "Request conflict"
            );
            case BUSINESS_RULE -> new HttpErrorMapping(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Business rule violation"
            );
            case INTERNAL -> new HttpErrorMapping(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error"
            );
        };
    }
}

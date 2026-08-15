package com.project.optrabidz.common.api.error;

public record ValidationViolation(String field, String message) {
    public ValidationViolation {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        field = field.strip();
        message = message.strip();
    }
}

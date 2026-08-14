package com.project.optrabidz.common.error;

public record ErrorDetail(String field, String issue) {
    public ErrorDetail {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        if (issue == null || issue.isBlank()) {
            throw new IllegalArgumentException("issue must not be blank");
        }
        field = field.strip();
        issue = issue.strip();
    }
}

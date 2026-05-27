package com.project.optrabidz.common.api.response;

import com.project.optrabidz.common.api.exception.ErrorField;

import java.util.List;

public record ErrorResponse(boolean success, ErrorBody error, Meta meta) {
    public ErrorResponse(ErrorBody error, Meta meta) {
        this(false, error, meta);
    }

    public record ErrorBody(String code, String message, List<ErrorField> fields) {
        public ErrorBody {
            fields = fields == null ? List.of() : List.copyOf(fields);
        }
    }
}

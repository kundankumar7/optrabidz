package com.project.optrabidz.common.api.response;

public record SuccessResponse<T>(boolean success, T data, Meta meta) {
    public SuccessResponse(T data, Meta meta) {
        this(true, data, meta);
    }
}

package com.project.optrabidz.common.api.response;

import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.common.api.exception.ErrorField;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApiResponse {
    public static final String REQUEST_ID_ATTRIBUTE = "optrabidz.requestId";

    private ApiResponse() {
    }

    public static <T> SuccessResponse<T> success(T data, HttpServletRequest request) {
        return new SuccessResponse<>(data, meta(request));
    }

    public static ErrorResponse error(ErrorCode errorCode,
                                      String message,
                                      List<ErrorField> fields,
                                      HttpServletRequest request) {
        return new ErrorResponse(
                new ErrorResponse.ErrorBody(errorCode.name(), message, fields),
                meta(request)
        );
    }

    public static Meta meta(HttpServletRequest request) {
        return new Meta(resolveRequestId(request), Instant.now());
    }

    private static String resolveRequestId(HttpServletRequest request) {
        if (request == null) {
            return UUID.randomUUID().toString();
        }

        Object requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (requestId instanceof String value && !value.isBlank()) {
            return value;
        }

        String generated = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, generated);
        return generated;
    }
}

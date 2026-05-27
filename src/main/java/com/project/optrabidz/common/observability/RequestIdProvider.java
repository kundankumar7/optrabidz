package com.project.optrabidz.common.observability;

import com.project.optrabidz.common.api.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class RequestIdProvider {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private RequestIdProvider() {
    }

    public static String resolveOrCreate(HttpServletRequest request) {
        if (request == null) {
            return UUID.randomUUID().toString();
        }

        Object existing = request.getAttribute(ApiResponse.REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String requestId && !requestId.isBlank()) {
            return requestId;
        }

        String inboundRequestId = request.getHeader(REQUEST_ID_HEADER);
        String requestId = isSafeRequestId(inboundRequestId)
                ? inboundRequestId.trim()
                : UUID.randomUUID().toString();

        request.setAttribute(ApiResponse.REQUEST_ID_ATTRIBUTE, requestId);
        return requestId;
    }

    private static boolean isSafeRequestId(String value) {
        return value != null
                && !value.isBlank()
                && value.length() <= 100
                && value.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == '.');
    }
}

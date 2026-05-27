package com.project.optrabidz.marketplace.application.dto.response;

import java.util.List;

public record RecommendationInfoResponse(
        int score,
        List<String> reasons
) {
    public RecommendationInfoResponse {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}

package com.project.optrabidz.marketplace.application.recommendation;

import com.project.optrabidz.marketplace.domain.model.FundingModel;

import java.math.BigDecimal;

public record RecommendationRequest(
        Long accountId,
        FundingModel fundingModel,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String currencyCode,
        int page,
        int size,
        int candidateLimit
) {
    public RecommendationRequest {
        page = Math.max(page, 1);
        size = Math.max(size, 1);
        candidateLimit = Math.max(candidateLimit, 1);
    }
}

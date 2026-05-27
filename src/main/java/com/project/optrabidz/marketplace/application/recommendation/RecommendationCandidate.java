package com.project.optrabidz.marketplace.application.recommendation;

import com.project.optrabidz.marketplace.application.dto.response.RecommendationInfoResponse;
import com.project.optrabidz.marketplace.domain.model.FundingListing;

import java.util.ArrayList;
import java.util.List;

public class RecommendationCandidate {
    private final FundingListing listing;
    private int score;
    private final List<String> reasons = new ArrayList<>();

    public RecommendationCandidate(FundingListing listing) {
        this.listing = listing;
    }

    public FundingListing listing() {
        return listing;
    }

    public int score() {
        return score;
    }

    public void addScore(int points, String reason) {
        if (points <= 0) {
            return;
        }
        score += points;
        addReason(reason);
    }

    public void addReason(String reason) {
        if (reason != null && !reason.isBlank()) {
            reasons.add(reason);
        }
    }

    public boolean hasReasons() {
        return !reasons.isEmpty();
    }

    public RecommendationInfoResponse toRecommendationInfo() {
        return new RecommendationInfoResponse(score, reasons);
    }
}

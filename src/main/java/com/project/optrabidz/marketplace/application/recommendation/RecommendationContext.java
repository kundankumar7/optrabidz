package com.project.optrabidz.marketplace.application.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RecommendationContext {
    private final RecommendationRequest request;
    private Set<String> investorPreferenceKeys = Set.of();
    private List<RecommendationCandidate> candidates = new ArrayList<>();

    public RecommendationContext(RecommendationRequest request) {
        this.request = request;
    }

    public RecommendationRequest request() {
        return request;
    }

    public Set<String> investorPreferenceKeys() {
        return investorPreferenceKeys;
    }

    public void investorPreferenceKeys(Set<String> investorPreferenceKeys) {
        this.investorPreferenceKeys = investorPreferenceKeys == null ? Set.of() : Set.copyOf(investorPreferenceKeys);
    }

    public List<RecommendationCandidate> candidates() {
        return candidates;
    }

    public void candidates(List<RecommendationCandidate> candidates) {
        this.candidates = candidates == null ? List.of() : new ArrayList<>(candidates);
    }
}

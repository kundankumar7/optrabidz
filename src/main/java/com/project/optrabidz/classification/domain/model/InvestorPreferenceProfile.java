package com.project.optrabidz.classification.domain.model;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

public class InvestorPreferenceProfile {
    private final Long investorId;
    private List<InvestorPreference> preferences;

    private InvestorPreferenceProfile(Long investorId, List<InvestorPreference> preferences) {
        Assert.notNull(investorId, "investorId must not be null");
        this.investorId = investorId;
        this.preferences = new ArrayList<>(preferences == null ? List.of() : preferences);
    }

    public static InvestorPreferenceProfile establish(Long investorId, List<InvestorPreference> preferences) {
        return new InvestorPreferenceProfile(investorId, preferences);
    }

    public void declare(String preferenceType, String preferenceValue) {
        if (contains(preferenceType, preferenceValue)) {
            throw new IllegalStateException("Preference already exists");
        }
        preferences.add(InvestorPreference.create(preferenceType, preferenceValue));
    }

    public void revoke(String preferenceType, String preferenceValue) {
        boolean removed = preferences.removeIf(preference ->
                sameEntry(preference, preferenceType, preferenceValue));
        if (!removed) {
            throw new IllegalStateException("Preference not found");
        }
    }

    public void replaceAll(List<InvestorPreference> preferences) {
        this.preferences = new ArrayList<>(preferences == null ? List.of() : preferences);
    }

    public boolean contains(String preferenceType, String preferenceValue) {
        return preferences.stream()
                .anyMatch(preference -> sameEntry(preference, preferenceType, preferenceValue));
    }

    private boolean sameEntry(InvestorPreference preference,
                              String preferenceType,
                              String preferenceValue) {
        InvestorPreference candidate = InvestorPreference.create(preferenceType, preferenceValue);
        return preference.getPreferenceType().equals(candidate.getPreferenceType())
                && preference.getPreferenceValue().equals(candidate.getPreferenceValue());
    }

    public Long getInvestorId() {
        return investorId;
    }

    public List<InvestorPreference> getPreferences() {
        return List.copyOf(preferences);
    }
}

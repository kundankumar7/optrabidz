package com.project.optrabidz.classification.domain.model;

import org.springframework.util.Assert;

import java.util.Locale;

public class InvestorPreference {
    private Long investorPreferenceId;
    private String preferenceType;
    private String preferenceValue;

    protected InvestorPreference() {
    }

    public InvestorPreference(Long investorPreferenceId,
                              String preferenceType,
                              String preferenceValue) {
        this.investorPreferenceId = investorPreferenceId;
        this.preferenceType = normalizeType(preferenceType);
        this.preferenceValue = normalizeValue(preferenceValue);
    }

    public static InvestorPreference create(String preferenceType, String preferenceValue) {
        return new InvestorPreference(null, preferenceType, preferenceValue);
    }

    private String normalizeType(String preferenceType) {
        Assert.hasText(preferenceType, "preferenceType must not be blank");
        return preferenceType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeValue(String preferenceValue) {
        Assert.hasText(preferenceValue, "preferenceValue must not be blank");
        return preferenceValue.trim();
    }

    public Long getInvestorPreferenceId() {
        return investorPreferenceId;
    }

    public String getPreferenceType() {
        return preferenceType;
    }

    public String getPreferenceValue() {
        return preferenceValue;
    }
}

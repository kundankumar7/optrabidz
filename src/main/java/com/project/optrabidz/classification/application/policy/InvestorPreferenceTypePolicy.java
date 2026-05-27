package com.project.optrabidz.classification.application.policy;

public interface InvestorPreferenceTypePolicy {
    boolean supports(String preferenceType);

    void validateValue(String preferenceValue);

    int maxAllowedPerType();
}

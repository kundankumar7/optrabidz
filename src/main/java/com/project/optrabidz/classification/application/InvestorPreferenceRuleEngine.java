package com.project.optrabidz.classification.application;

import com.project.optrabidz.classification.application.specification.InvestorPreferenceSpecification;
import com.project.optrabidz.classification.domain.model.InvestorPreference;
import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvestorPreferenceRuleEngine {
    private final List<InvestorPreferenceSpecification> specifications;

    public InvestorPreferenceRuleEngine(List<InvestorPreferenceSpecification> specifications) {
        this.specifications = specifications;
    }

    public void validateBeforeAdd(InvestorPreferenceProfile profile, InvestorPreference entry) {
        InvestorPreferenceProfile candidate = InvestorPreferenceProfile.establish(
                profile.getInvestorId(),
                profile.getPreferences()
        );
        candidate.declare(entry.getPreferenceType(), entry.getPreferenceValue());
        validate(candidate);
    }

    public void validateBeforeReplace(InvestorPreferenceProfile profile,
                                      List<InvestorPreference> entries) {
        InvestorPreferenceProfile candidate = InvestorPreferenceProfile.establish(
                profile.getInvestorId(),
                entries
        );
        validate(candidate);
    }

    public void validateBeforeRemove(InvestorPreferenceProfile profile,
                                     String preferenceType,
                                     String preferenceValue) {
        InvestorPreferenceProfile candidate = InvestorPreferenceProfile.establish(
                profile.getInvestorId(),
                profile.getPreferences()
        );
        candidate.revoke(preferenceType, preferenceValue);
        validate(candidate);
    }

    private void validate(InvestorPreferenceProfile profile) {
        specifications.forEach(specification -> specification.validate(profile));
    }
}

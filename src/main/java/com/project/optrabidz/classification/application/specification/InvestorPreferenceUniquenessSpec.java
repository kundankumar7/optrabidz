package com.project.optrabidz.classification.application.specification;

import com.project.optrabidz.classification.application.exception.InvestorPreferenceAlreadyExistsException;
import com.project.optrabidz.classification.domain.model.InvestorPreference;
import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.HashSet;

@Component
public class InvestorPreferenceUniquenessSpec implements InvestorPreferenceSpecification {
    @Override
    public void validate(InvestorPreferenceProfile profile) {
        Set<String> uniqueEntries = new HashSet<>();
        profile.getPreferences().stream()
                .filter(preference -> !uniqueEntries.add(key(preference)))
                .findFirst()
                .ifPresent(preference -> {
                    throw new InvestorPreferenceAlreadyExistsException(
                            preference.getPreferenceType(),
                            preference.getPreferenceValue()
                    );
                });
    }

    private String key(InvestorPreference preference) {
        return preference.getPreferenceType() + "::" + preference.getPreferenceValue();
    }
}

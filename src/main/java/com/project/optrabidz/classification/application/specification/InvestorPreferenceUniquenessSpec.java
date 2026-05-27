package com.project.optrabidz.classification.application.specification;

import com.project.optrabidz.classification.application.exception.ClassificationAlreadyExistsException;
import com.project.optrabidz.classification.domain.model.InvestorPreference;
import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class InvestorPreferenceUniquenessSpec implements InvestorPreferenceSpecification {
    @Override
    public void validate(InvestorPreferenceProfile profile) {
        Set<String> uniqueEntries = profile.getPreferences().stream()
                .map(this::key)
                .collect(Collectors.toSet());
        if (uniqueEntries.size() != profile.getPreferences().size()) {
            throw new ClassificationAlreadyExistsException("Duplicate investor preference is not allowed");
        }
    }

    private String key(InvestorPreference preference) {
        return preference.getPreferenceType() + "::" + preference.getPreferenceValue();
    }
}

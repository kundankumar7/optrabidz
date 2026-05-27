package com.project.optrabidz.classification.application.specification;

import com.project.optrabidz.classification.application.exception.InvalidClassificationException;
import com.project.optrabidz.classification.application.policy.InvestorPreferenceTypePolicy;
import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class InvestorPreferenceIntegritySpec implements InvestorPreferenceSpecification {
    private final List<InvestorPreferenceTypePolicy> policies;

    public InvestorPreferenceIntegritySpec(List<InvestorPreferenceTypePolicy> policies) {
        this.policies = policies;
    }

    @Override
    public void validate(InvestorPreferenceProfile profile) {
        profile.getPreferences().forEach(preference -> {
            if (!StringUtils.hasText(preference.getPreferenceType())) {
                throw new InvalidClassificationException("Preference type must not be blank");
            }
            InvestorPreferenceTypePolicy policy = resolvePolicy(preference.getPreferenceType());
            policy.validateValue(preference.getPreferenceValue());
        });
    }

    private InvestorPreferenceTypePolicy resolvePolicy(String preferenceType) {
        return policies.stream()
                .filter(policy -> policy.supports(preferenceType))
                .findFirst()
                .orElseThrow(() -> new InvalidClassificationException(
                        "Unsupported investor preference type: " + preferenceType
                ));
    }
}

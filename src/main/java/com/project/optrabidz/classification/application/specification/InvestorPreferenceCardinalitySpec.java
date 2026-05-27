package com.project.optrabidz.classification.application.specification;

import com.project.optrabidz.classification.application.exception.InvalidClassificationException;
import com.project.optrabidz.classification.application.policy.InvestorPreferenceTypePolicy;
import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InvestorPreferenceCardinalitySpec implements InvestorPreferenceSpecification {
    private final List<InvestorPreferenceTypePolicy> policies;

    public InvestorPreferenceCardinalitySpec(List<InvestorPreferenceTypePolicy> policies) {
        this.policies = policies;
    }

    @Override
    public void validate(InvestorPreferenceProfile profile) {
        Map<String, Long> countsByType = profile.getPreferences().stream()
                .collect(Collectors.groupingBy(
                        preference -> preference.getPreferenceType(),
                        Collectors.counting()
                ));

        countsByType.forEach((type, count) -> {
            int maxAllowed = resolvePolicy(type).maxAllowedPerType();
            if (count > maxAllowed) {
                throw new InvalidClassificationException(
                        "Investor preference cardinality exceeded for type: " + type
                );
            }
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

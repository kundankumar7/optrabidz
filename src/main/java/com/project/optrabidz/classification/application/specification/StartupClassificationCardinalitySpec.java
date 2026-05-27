package com.project.optrabidz.classification.application.specification;

import com.project.optrabidz.classification.application.exception.InvalidClassificationException;
import com.project.optrabidz.classification.application.policy.StartupClassificationTypePolicy;
import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StartupClassificationCardinalitySpec implements StartupClassificationSpecification {
    private final List<StartupClassificationTypePolicy> policies;

    public StartupClassificationCardinalitySpec(List<StartupClassificationTypePolicy> policies) {
        this.policies = policies;
    }

    @Override
    public void validate(StartupClassificationProfile profile) {
        Map<String, Long> countsByType = profile.getClassifications().stream()
                .collect(Collectors.groupingBy(
                        classification -> classification.getClassificationType(),
                        Collectors.counting()
                ));

        countsByType.forEach((type, count) -> {
            int maxAllowed = resolvePolicy(type).maxAllowedPerType();
            if (count > maxAllowed) {
                throw new InvalidClassificationException(
                        "Startup classification cardinality exceeded for type: " + type
                );
            }
        });
    }

    private StartupClassificationTypePolicy resolvePolicy(String classificationType) {
        return policies.stream()
                .filter(policy -> policy.supports(classificationType))
                .findFirst()
                .orElseThrow(() -> new InvalidClassificationException(
                        "Unsupported startup classification type: " + classificationType
                ));
    }
}

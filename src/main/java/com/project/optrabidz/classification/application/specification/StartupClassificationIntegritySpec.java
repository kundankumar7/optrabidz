package com.project.optrabidz.classification.application.specification;

import com.project.optrabidz.classification.application.exception.InvalidClassificationException;
import com.project.optrabidz.classification.application.policy.StartupClassificationTypePolicy;
import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class StartupClassificationIntegritySpec implements StartupClassificationSpecification {
    private final List<StartupClassificationTypePolicy> policies;

    public StartupClassificationIntegritySpec(List<StartupClassificationTypePolicy> policies) {
        this.policies = policies;
    }

    @Override
    public void validate(StartupClassificationProfile profile) {
        profile.getClassifications().forEach(classification -> {
            if (!StringUtils.hasText(classification.getClassificationType())) {
                throw new InvalidClassificationException("Classification type must not be blank");
            }
            StartupClassificationTypePolicy policy = resolvePolicy(classification.getClassificationType());
            policy.validateValue(classification.getClassificationValue());
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

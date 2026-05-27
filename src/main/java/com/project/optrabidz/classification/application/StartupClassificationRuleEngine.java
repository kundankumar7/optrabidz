package com.project.optrabidz.classification.application;

import com.project.optrabidz.classification.domain.model.StartupClassification;
import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;
import com.project.optrabidz.classification.application.specification.StartupClassificationSpecification;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartupClassificationRuleEngine {
    private final List<StartupClassificationSpecification> specifications;

    public StartupClassificationRuleEngine(List<StartupClassificationSpecification> specifications) {
        this.specifications = specifications;
    }

    public void validateBeforeAdd(StartupClassificationProfile profile, StartupClassification entry) {
        StartupClassificationProfile candidate = StartupClassificationProfile.establish(
                profile.getStartupId(),
                profile.getClassifications()
        );
        candidate.declare(entry.getClassificationType(), entry.getClassificationValue());
        validate(candidate);
    }

    public void validateBeforeReplace(StartupClassificationProfile profile,
                                      List<StartupClassification> entries) {
        StartupClassificationProfile candidate = StartupClassificationProfile.establish(
                profile.getStartupId(),
                entries
        );
        validate(candidate);
    }

    public void validateBeforeRemove(StartupClassificationProfile profile,
                                     String classificationType,
                                     String classificationValue) {
        StartupClassificationProfile candidate = StartupClassificationProfile.establish(
                profile.getStartupId(),
                profile.getClassifications()
        );
        candidate.revoke(classificationType, classificationValue);
        validate(candidate);
    }

    private void validate(StartupClassificationProfile profile) {
        specifications.forEach(specification -> specification.validate(profile));
    }
}

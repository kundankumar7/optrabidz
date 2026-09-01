package com.project.optrabidz.classification.application.specification;

import com.project.optrabidz.classification.application.exception.StartupClassificationAlreadyExistsException;
import com.project.optrabidz.classification.domain.model.StartupClassification;
import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.HashSet;

@Component
public class StartupClassificationUniquenessSpec implements StartupClassificationSpecification {
    @Override
    public void validate(StartupClassificationProfile profile) {
        Set<String> uniqueEntries = new HashSet<>();
        profile.getClassifications().stream()
                .filter(classification -> !uniqueEntries.add(key(classification)))
                .findFirst()
                .ifPresent(classification -> {
                    throw new StartupClassificationAlreadyExistsException(
                            classification.getClassificationType(),
                            classification.getClassificationValue()
                    );
                });
    }

    private String key(StartupClassification classification) {
        return classification.getClassificationType() + "::" + classification.getClassificationValue();
    }
}

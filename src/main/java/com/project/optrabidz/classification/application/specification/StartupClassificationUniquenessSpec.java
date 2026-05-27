package com.project.optrabidz.classification.application.specification;

import com.project.optrabidz.classification.application.exception.ClassificationAlreadyExistsException;
import com.project.optrabidz.classification.domain.model.StartupClassification;
import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StartupClassificationUniquenessSpec implements StartupClassificationSpecification {
    @Override
    public void validate(StartupClassificationProfile profile) {
        Set<String> uniqueEntries = profile.getClassifications().stream()
                .map(this::key)
                .collect(Collectors.toSet());
        if (uniqueEntries.size() != profile.getClassifications().size()) {
            throw new ClassificationAlreadyExistsException("Duplicate startup classification is not allowed");
        }
    }

    private String key(StartupClassification classification) {
        return classification.getClassificationType() + "::" + classification.getClassificationValue();
    }
}

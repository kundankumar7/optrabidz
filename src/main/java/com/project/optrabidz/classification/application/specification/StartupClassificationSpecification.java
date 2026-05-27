package com.project.optrabidz.classification.application.specification;

import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;

public interface StartupClassificationSpecification {
    void validate(StartupClassificationProfile profile);
}

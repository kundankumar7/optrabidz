package com.project.optrabidz.classification.application.specification;

import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;

public interface InvestorPreferenceSpecification {
    void validate(InvestorPreferenceProfile profile);
}

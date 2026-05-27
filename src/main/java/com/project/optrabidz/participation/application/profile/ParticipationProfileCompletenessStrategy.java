package com.project.optrabidz.participation.application.profile;

import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.RoleType;

public interface ParticipationProfileCompletenessStrategy {
    boolean supports(RoleType roleType);

    ProfileStatus evaluate(Long accountId);
}

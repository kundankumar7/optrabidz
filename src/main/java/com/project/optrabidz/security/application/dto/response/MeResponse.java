package com.project.optrabidz.security.application.dto.response;

import com.project.optrabidz.identity.domain.model.AccountState;
import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.RoleType;

public record MeResponse(
        RoleType role,
        AccountState accountState,
        ProfileStatus profileStatus,
        RoleType actorType,
        boolean actorExists
) {
}

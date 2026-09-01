package com.project.optrabidz.participation.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.application.error.ParticipationErrors;

public final class ParticipationAuthorizationException extends ApplicationException {

    public ParticipationAuthorizationException(RoleType actualRole, RoleType expectedRole) {
        super(
                ParticipationErrors.AUTHORIZATION_FAILED,
                "PARTICIPATION.AUTHORIZATION.FAILED",
                "Role " + actualRole + " cannot perform an operation requiring " + expectedRole
        );
    }
}

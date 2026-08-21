package com.project.optrabidz.security.application.exception;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.security.application.error.SecurityErrors;

public final class SelfRegistrationNotAllowedException extends ApplicationException {

    public SelfRegistrationNotAllowedException(RoleType roleType) {
        super(
                SecurityErrors.SELF_REGISTRATION_NOT_ALLOWED,
                "SECURITY.REGISTRATION.ROLE_NOT_ALLOWED",
                "Self-registration rejected for role " + roleType
        );
    }
}

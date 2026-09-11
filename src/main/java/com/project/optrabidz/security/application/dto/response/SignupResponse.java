package com.project.optrabidz.security.application.dto.response;

import com.project.optrabidz.identity.domain.model.RoleType;

public record SignupResponse(Long accountId, RoleType role) {
}

package com.project.optrabidz.security.application.dto.response;

import com.project.optrabidz.identity.domain.model.RoleType;

public record LoginResponse(Long accountId, RoleType role) {
}

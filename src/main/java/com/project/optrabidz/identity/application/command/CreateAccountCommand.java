package com.project.optrabidz.identity.application.command;

import com.project.optrabidz.identity.domain.model.RoleType;

public record CreateAccountCommand(RoleType roleType) {
}

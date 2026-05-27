package com.project.optrabidz.identity.application.command;

import com.project.optrabidz.identity.domain.model.ProfileStatus;

public record UpdateProfileStatusCommand(Long accountId, ProfileStatus profileStatus) {
}

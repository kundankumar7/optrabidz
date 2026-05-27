package com.project.optrabidz.security.application.command;

public record ProvisionCredentialCommand(
        Long accountId,
        String email,
        String rawPassword
) {
}

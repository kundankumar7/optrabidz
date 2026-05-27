package com.project.optrabidz.governance.application.admin;

public record BootstrapAdminCommand(
        String email,
        String rawPassword,
        String publicDisplayName,
        String organizationLabel
) {
}

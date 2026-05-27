package com.project.optrabidz.participation.application.dto.request;

public record CreateAdminRequest(
        String publicDisplayName,
        String organizationLabel
) {
}

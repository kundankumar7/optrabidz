package com.project.optrabidz.participation.application.dto.response;

public record AdminResponse(
        Long adminId,
        String publicDisplayName,
        String organizationLabel
) {
}

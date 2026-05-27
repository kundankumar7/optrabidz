package com.project.optrabidz.participation.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateStartupRequest(
        @NotBlank String legalEntityName,
        @NotBlank String incorporationCountryCode,
        @NotBlank String publicDisplayName,
        @NotBlank String businessDescription,
        List<@NotBlank String> webPresences,
        List<@Valid LegalRegistrationRequest> legalRegistrations
) {
    public record LegalRegistrationRequest(
            @NotBlank String type,
            @NotBlank String value
    ) {
    }
}

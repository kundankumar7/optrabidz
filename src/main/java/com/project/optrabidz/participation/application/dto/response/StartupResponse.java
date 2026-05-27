package com.project.optrabidz.participation.application.dto.response;

import java.util.List;

public record StartupResponse(
        Long startupId,
        String legalEntityName,
        String incorporationCountryCode,
        String publicDisplayName,
        String businessDescription,
        List<String> webPresences,
        List<LegalRegistrationResponse> legalRegistrations
) {
    public record LegalRegistrationResponse(String type, String value) {
    }
}

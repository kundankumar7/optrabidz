package com.project.optrabidz.participation.application.dto.response;

import java.util.List;

public record InvestorResponse(
        Long investorId,
        String publicDisplayName,
        String investorDescription,
        String legalEntityName,
        List<String> webPresences
) {
}

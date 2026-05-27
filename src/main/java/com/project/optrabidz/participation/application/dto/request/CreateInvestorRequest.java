package com.project.optrabidz.participation.application.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateInvestorRequest(
        @NotBlank String publicDisplayName,
        @NotBlank String investorDescription,
        @NotBlank String legalEntityName,
        List<@NotBlank String> webPresences
) {
}

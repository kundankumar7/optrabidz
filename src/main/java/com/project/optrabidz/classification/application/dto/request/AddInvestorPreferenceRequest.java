package com.project.optrabidz.classification.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddInvestorPreferenceRequest(
        @NotBlank String preferenceType,
        @NotBlank String preferenceValue
) {
}

package com.project.optrabidz.classification.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddStartupClassificationRequest(
        @NotBlank String classificationType,
        @NotBlank String classificationValue
) {
}

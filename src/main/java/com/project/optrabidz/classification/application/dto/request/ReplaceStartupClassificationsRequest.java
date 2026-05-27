package com.project.optrabidz.classification.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceStartupClassificationsRequest(
        @NotNull List<@Valid AddStartupClassificationRequest> classifications
) {
}

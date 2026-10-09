package com.project.optrabidz.financial.application.dto.request;

import com.project.optrabidz.financial.domain.model.DemoPaymentOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DemoPaymentOutcomeRequest(
        @NotNull DemoPaymentOutcome outcome,
        @NotBlank @Size(max = 120) String idempotencyKey
) {
}

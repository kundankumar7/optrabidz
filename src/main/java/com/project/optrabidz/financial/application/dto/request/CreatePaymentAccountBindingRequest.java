package com.project.optrabidz.financial.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePaymentAccountBindingRequest(
        @NotBlank @Size(max = 120) String idempotencyKey
) {
}

package com.project.optrabidz.financial.application.dto.response;

public record PaymentAccountBindingReadinessResponse(
        boolean ready,
        PaymentAccountBindingResponse binding
) {
}

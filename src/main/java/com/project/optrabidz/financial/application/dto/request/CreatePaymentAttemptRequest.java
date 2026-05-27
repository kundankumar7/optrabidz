package com.project.optrabidz.financial.application.dto.request;

import com.project.optrabidz.financial.domain.model.PaymentMethodType;

public record CreatePaymentAttemptRequest(
        String providerCode,
        PaymentMethodType methodType
) {
}

package com.project.optrabidz.financial.application.dto.response;

import com.project.optrabidz.financial.domain.model.PaymentAttemptState;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;

import java.math.BigDecimal;

public record DemoCheckoutResponse(
        Long paymentAttemptId,
        Long paymentIntentId,
        String providerCode,
        BigDecimal amount,
        String currencyCode,
        PaymentPurpose paymentPurpose,
        PaymentMethodType methodType,
        PaymentAttemptState attemptState,
        String environmentLabel,
        String warning
) {
}

package com.project.optrabidz.financial.application.strategy;

import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;

import java.time.Instant;

public interface PaymentMethodStrategy {
    boolean supports(String providerCode, PaymentMethodType methodType);

    PaymentAttempt initiate(PaymentIntent paymentIntent, PaymentAttempt paymentAttempt, Instant now);
}

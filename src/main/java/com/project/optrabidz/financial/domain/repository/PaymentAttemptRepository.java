package com.project.optrabidz.financial.domain.repository;

import com.project.optrabidz.financial.domain.model.PaymentAttempt;

import java.time.Instant;
import java.util.Optional;

public interface PaymentAttemptRepository {
    PaymentAttempt save(PaymentAttempt paymentAttempt);

    Optional<PaymentAttempt> findById(Long paymentAttemptId);

    Optional<PaymentAttempt> findByIdForPayer(Long paymentAttemptId, Long payerAccountId);

    Optional<PaymentAttempt> findByIdForProvider(Long paymentAttemptId, String providerCode);

    int confirmActive(Long paymentAttemptId, String providerPaymentId, Instant now);

    int failActive(Long paymentAttemptId, String failureCode, String failureMessage, Instant now);
}

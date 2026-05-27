package com.project.optrabidz.financial.domain.repository;

import com.project.optrabidz.financial.domain.model.PaymentIntent;

import java.time.Instant;
import java.util.Optional;

public interface PaymentIntentRepository {
    PaymentIntent save(PaymentIntent paymentIntent);

    Optional<PaymentIntent> findById(Long paymentIntentId);

    Optional<PaymentIntent> findActiveBySettlementId(Long settlementId);

    Optional<PaymentIntent> findActiveByRepaymentInstallmentId(Long repaymentInstallmentId);

    PaymentIntent saveNewOrFindActiveBySettlement(PaymentIntent paymentIntent);

    PaymentIntent saveNewOrFindActiveByRepaymentInstallment(PaymentIntent paymentIntent);

    java.util.List<Long> findExpiredActiveRepaymentInstallmentIds(Instant now, int batchSize);

    int confirmActive(Long paymentIntentId, Instant now);

    int failActive(Long paymentIntentId, String failureCode, String failureMessage, Instant now);

    int expireExpiredActive(Instant now, int batchSize);
}

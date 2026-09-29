package com.project.optrabidz.financial.domain.repository;

import com.project.optrabidz.financial.domain.model.ExpiredPaymentIntentReference;
import com.project.optrabidz.financial.domain.model.PaymentIntent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentIntentRepository {
    PaymentIntent save(PaymentIntent paymentIntent);

    Optional<PaymentIntent> findById(Long paymentIntentId);

    Optional<PaymentIntent> findByIdForParticipant(Long paymentIntentId, Long accountId);

    Optional<PaymentIntent> findByIdForPayer(Long paymentIntentId, Long payerAccountId);

    Optional<PaymentIntent> findActiveBySettlementId(Long settlementId);

    Optional<PaymentIntent> findActiveByRepaymentInstallmentId(Long repaymentInstallmentId);

    PaymentIntent saveNewOrFindActiveBySettlement(PaymentIntent paymentIntent);

    PaymentIntent saveNewOrFindActiveByRepaymentInstallment(PaymentIntent paymentIntent);

    int confirmActive(Long paymentIntentId, Instant now);

    int failActive(Long paymentIntentId, String failureCode, String failureMessage, Instant now);

    List<ExpiredPaymentIntentReference> expireExpiredActiveReturning(Instant now, int batchSize);

    Optional<ExpiredPaymentIntentReference> expireActiveByIdReturning(Long paymentIntentId, Instant now);
}

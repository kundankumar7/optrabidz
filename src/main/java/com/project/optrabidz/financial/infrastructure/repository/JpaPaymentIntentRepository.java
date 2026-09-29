package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.infrastructure.entity.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface JpaPaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {
    @Query("""
            select paymentIntent
            from PaymentIntent paymentIntent
            where paymentIntent.paymentIntentId = :paymentIntentId
              and (paymentIntent.payerAccountId = :accountId
                   or paymentIntent.payeeAccountId = :accountId)
            """)
    Optional<PaymentIntent> findForParticipant(@Param("paymentIntentId") Long paymentIntentId,
                                               @Param("accountId") Long accountId);

    Optional<PaymentIntent> findByPaymentIntentIdAndPayerAccountId(Long paymentIntentId, Long payerAccountId);

    Optional<PaymentIntent> findFirstBySettlementIdAndPaymentStateInOrderByCreatedAtDesc(
            Long settlementId,
            Collection<com.project.optrabidz.financial.domain.model.PaymentState> paymentStates
    );

    Optional<PaymentIntent> findFirstByRepaymentInstallmentIdAndPaymentStateInOrderByCreatedAtDesc(
            Long repaymentInstallmentId,
            Collection<com.project.optrabidz.financial.domain.model.PaymentState> paymentStates
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update payment_intent
            set payment_state = 'PAYMENT_CONFIRMED',
                confirmed_at = :now
            where payment_intent_id = :paymentIntentId
              and payment_state in (
                'CREATED'::payment_state_enum,
                'PAYMENT_PENDING'::payment_state_enum
              )
              and expires_at > :now
            """, nativeQuery = true)
    int confirmActive(@Param("paymentIntentId") Long paymentIntentId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update payment_intent
            set payment_state = 'PAYMENT_FAILED',
                failure_code = :failureCode,
                failure_message = :failureMessage,
                failed_at = :now
            where payment_intent_id = :paymentIntentId
              and payment_state in (
                'CREATED'::payment_state_enum,
                'PAYMENT_PENDING'::payment_state_enum
              )
              and expires_at > :now
            """, nativeQuery = true)
    int failActive(@Param("paymentIntentId") Long paymentIntentId,
                   @Param("failureCode") String failureCode,
                   @Param("failureMessage") String failureMessage,
                   @Param("now") Instant now);
}

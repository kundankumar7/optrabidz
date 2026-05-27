package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.infrastructure.entity.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaPaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {
    Optional<PaymentIntent> findFirstBySettlementIdAndPaymentStateInOrderByCreatedAtDesc(
            Long settlementId,
            Collection<com.project.optrabidz.financial.domain.model.PaymentState> paymentStates
    );

    Optional<PaymentIntent> findFirstByRepaymentInstallmentIdAndPaymentStateInOrderByCreatedAtDesc(
            Long repaymentInstallmentId,
            Collection<com.project.optrabidz.financial.domain.model.PaymentState> paymentStates
    );

    @Query(value = """
            select distinct repayment_installment_id
            from payment_intent
            where payment_purpose = 'REPAYMENT'::payment_purpose_enum
              and repayment_installment_id is not null
              and payment_state in (
                'CREATED'::payment_state_enum,
                'PAYMENT_PENDING'::payment_state_enum
              )
              and expires_at <= :now
            order by repayment_installment_id asc
            limit :batchSize
            """, nativeQuery = true)
    List<Long> findExpiredActiveRepaymentInstallmentIds(@Param("now") Instant now, @Param("batchSize") int batchSize);

    @Query(value = """
            select payment_intent_id
            from payment_intent
            where payment_state in (
                'CREATED'::payment_state_enum,
                'PAYMENT_PENDING'::payment_state_enum
            )
              and expires_at <= :now
            order by expires_at asc, payment_intent_id asc
            for update skip locked
            limit :batchSize
            """, nativeQuery = true)
    List<Long> findExpiredActiveIds(@Param("now") Instant now, @Param("batchSize") int batchSize);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update payment_intent
            set payment_state = 'PAYMENT_EXPIRED',
                expired_at = :now
            where payment_intent_id in (:paymentIntentIds)
              and payment_state in (
                'CREATED'::payment_state_enum,
                'PAYMENT_PENDING'::payment_state_enum
              )
              and expires_at <= :now
            """, nativeQuery = true)
    int expireActive(@Param("paymentIntentIds") List<Long> paymentIntentIds, @Param("now") Instant now);

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

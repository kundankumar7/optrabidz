package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.infrastructure.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface JpaPaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
    @Query("""
            select paymentAttempt
            from PaymentAttempt paymentAttempt
            where paymentAttempt.paymentAttemptId = :paymentAttemptId
              and exists (
                  select paymentIntent.paymentIntentId
                  from PaymentIntent paymentIntent
                  where paymentIntent.paymentIntentId = paymentAttempt.paymentIntentId
                    and paymentIntent.payerAccountId = :payerAccountId
              )
            """)
    Optional<PaymentAttempt> findForPayer(@Param("paymentAttemptId") Long paymentAttemptId,
                                          @Param("payerAccountId") Long payerAccountId);

    Optional<PaymentAttempt> findByPaymentAttemptIdAndProviderCodeIgnoreCase(Long paymentAttemptId,
                                                                             String providerCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update payment_attempt
            set attempt_state = 'CONFIRMED',
                provider_payment_id = :providerPaymentId,
                confirmed_at = :now
            where payment_attempt_id = :paymentAttemptId
              and attempt_state in (
                'CREATED'::payment_attempt_state_enum,
                'INITIATED'::payment_attempt_state_enum,
                'REQUIRES_ACTION'::payment_attempt_state_enum
              )
            """, nativeQuery = true)
    int confirmActive(@Param("paymentAttemptId") Long paymentAttemptId,
                      @Param("providerPaymentId") String providerPaymentId,
                      @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update payment_attempt
            set attempt_state = 'FAILED',
                failure_code = :failureCode,
                failure_message = :failureMessage,
                failed_at = :now
            where payment_attempt_id = :paymentAttemptId
              and attempt_state in (
                'CREATED'::payment_attempt_state_enum,
                'INITIATED'::payment_attempt_state_enum,
                'REQUIRES_ACTION'::payment_attempt_state_enum
              )
            """, nativeQuery = true)
    int failActive(@Param("paymentAttemptId") Long paymentAttemptId,
                   @Param("failureCode") String failureCode,
                   @Param("failureMessage") String failureMessage,
                   @Param("now") Instant now);
}

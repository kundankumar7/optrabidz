package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.infrastructure.entity.PayoutTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface JpaPayoutTransferRepository extends JpaRepository<PayoutTransfer, Long> {
    Optional<PayoutTransfer> findByPaymentIntentId(Long paymentIntentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update payout_transfer
            set transfer_status = 'PROCESSING',
                attempt_count = attempt_count + 1,
                last_attempt_at = :now,
                updated_at = :now
            where payout_transfer_id = :payoutTransferId
              and cast(:expectedStatus as text) in ('PENDING', 'FAILED')
              and transfer_status = cast(:expectedStatus as payout_transfer_status_enum)
              and updated_at <= :now
            """, nativeQuery = true)
    int claimForProcessing(@Param("payoutTransferId") Long payoutTransferId,
                           @Param("expectedStatus") String expectedStatus,
                           @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update payout_transfer
            set transfer_status = 'CONFIRMED',
                provider_transfer_reference = :providerReference,
                confirmed_at = :now,
                updated_at = :now
            where payout_transfer_id = :payoutTransferId
              and transfer_status = 'PROCESSING'::payout_transfer_status_enum
              and last_attempt_at <= :now
              and updated_at <= :now
            """, nativeQuery = true)
    int markConfirmed(@Param("payoutTransferId") Long payoutTransferId,
                      @Param("providerReference") String providerReference,
                      @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update payout_transfer
            set transfer_status = 'FAILED',
                latest_failure_at = :now,
                latest_failure_code = :failureCode,
                latest_failure_message = :failureMessage,
                updated_at = :now
            where payout_transfer_id = :payoutTransferId
              and transfer_status = 'PROCESSING'::payout_transfer_status_enum
              and last_attempt_at <= :now
              and updated_at <= :now
            """, nativeQuery = true)
    int markFailed(@Param("payoutTransferId") Long payoutTransferId,
                   @Param("failureCode") String failureCode,
                   @Param("failureMessage") String failureMessage,
                   @Param("now") Instant now);
}

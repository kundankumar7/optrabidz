package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.infrastructure.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JpaSettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByAgreementId(Long agreementId);

    Page<Settlement> findByStartupId(Long startupId, Pageable pageable);

    Page<Settlement> findByInvestorId(Long investorId, Pageable pageable);

    @Query(value = """
            select settlement_id
            from settlement
            where settlement_state = 'SETTLEMENT_PENDING'::settlement_state_enum
              and expires_at <= :now
            order by expires_at asc, settlement_id asc
            for update skip locked
            limit :batchSize
            """, nativeQuery = true)
    List<Long> findExpiredPendingIds(@Param("now") Instant now, @Param("batchSize") int batchSize);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update settlement
            set settlement_state = 'SETTLEMENT_EXPIRED',
                expired_at = :now
            where settlement_id in (:settlementIds)
              and settlement_state = 'SETTLEMENT_PENDING'::settlement_state_enum
              and expires_at <= :now
            """, nativeQuery = true)
    int expirePending(@Param("settlementIds") List<Long> settlementIds, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update settlement
            set settlement_state = 'SETTLEMENT_CONFIRMED',
                confirmed_payment_intent_id = :paymentIntentId,
                confirmed_at = :now
            where settlement_id = :settlementId
              and settlement_state = 'SETTLEMENT_PENDING'::settlement_state_enum
              and expires_at > :now
            """, nativeQuery = true)
    int confirmPending(@Param("settlementId") Long settlementId,
                       @Param("paymentIntentId") Long paymentIntentId,
                       @Param("now") Instant now);
}

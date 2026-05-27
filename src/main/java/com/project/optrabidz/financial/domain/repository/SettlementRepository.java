package com.project.optrabidz.financial.domain.repository;

import com.project.optrabidz.financial.domain.model.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface SettlementRepository {
    Settlement save(Settlement settlement);

    Optional<Settlement> findById(Long settlementId);

    Optional<Settlement> findByAgreementId(Long agreementId);

    Page<Settlement> findByStartupId(Long startupId, Pageable pageable);

    Page<Settlement> findByInvestorId(Long investorId, Pageable pageable);

    int confirmPending(Long settlementId, Long paymentIntentId, Instant now);

    int expireExpiredPending(Instant now, int batchSize);
}

package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.Settlement;
import com.project.optrabidz.financial.domain.repository.SettlementRepository;
import com.project.optrabidz.financial.infrastructure.mapper.FinancialPersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class SettlementRepositoryAdapter implements SettlementRepository {
    private final JpaSettlementRepository jpaSettlementRepository;
    private final FinancialPersistenceMapper mapper;

    public SettlementRepositoryAdapter(JpaSettlementRepository jpaSettlementRepository,
                                       FinancialPersistenceMapper mapper) {
        this.jpaSettlementRepository = jpaSettlementRepository;
        this.mapper = mapper;
    }

    @Override
    public Settlement save(Settlement settlement) {
        return mapper.toDomain(jpaSettlementRepository.save(mapper.toEntity(settlement)));
    }

    @Override
    public Optional<Settlement> findById(Long settlementId) {
        return jpaSettlementRepository.findById(settlementId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Settlement> findByAgreementId(Long agreementId) {
        return jpaSettlementRepository.findByAgreementId(agreementId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Settlement> findByStartupId(Long startupId, Pageable pageable) {
        return jpaSettlementRepository.findByStartupId(startupId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Settlement> findByInvestorId(Long investorId, Pageable pageable) {
        return jpaSettlementRepository.findByInvestorId(investorId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public int confirmPending(Long settlementId, Long paymentIntentId, Instant now) {
        return jpaSettlementRepository.confirmPending(settlementId, paymentIntentId, now);
    }

    @Override
    @Transactional
    public int expireExpiredPending(Instant now, int batchSize) {
        if (batchSize <= 0) {
            return 0;
        }
        List<Long> ids = jpaSettlementRepository.findExpiredPendingIds(now, batchSize);
        if (ids.isEmpty()) {
            return 0;
        }
        return jpaSettlementRepository.expirePending(ids, now);
    }
}

package com.project.optrabidz.marketplace.infrastructure.repository;

import com.project.optrabidz.marketplace.domain.model.Agreement;
import com.project.optrabidz.marketplace.domain.repository.AgreementRepository;
import com.project.optrabidz.marketplace.infrastructure.mapper.MarketplacePersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AgreementRepositoryAdapter implements AgreementRepository {
    private final JpaAgreementRepository jpaAgreementRepository;
    private final MarketplacePersistenceMapper mapper;

    public AgreementRepositoryAdapter(JpaAgreementRepository jpaAgreementRepository,
                                      MarketplacePersistenceMapper mapper) {
        this.jpaAgreementRepository = jpaAgreementRepository;
        this.mapper = mapper;
    }

    @Override
    public Agreement save(Agreement agreement) {
        return mapper.toDomain(jpaAgreementRepository.save(mapper.toEntity(agreement)));
    }

    @Override
    public Optional<Agreement> findById(Long agreementId) {
        return jpaAgreementRepository.findById(agreementId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Agreement> findByBidId(Long bidId) {
        return jpaAgreementRepository.findByBidId(bidId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Agreement> findByStartupId(Long startupId, Pageable pageable) {
        return jpaAgreementRepository.findByStartupId(startupId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Agreement> findByInvestorId(Long investorId, Pageable pageable) {
        return jpaAgreementRepository.findByInvestorId(investorId, pageable)
                .map(mapper::toDomain);
    }
}

package com.project.optrabidz.participation.infrastructure.repository;

import com.project.optrabidz.participation.domain.model.Investor;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import com.project.optrabidz.participation.infrastructure.mapper.ParticipationPersistenceMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class InvestorRepositoryAdapter implements InvestorRepository {
    private final JpaInvestorRepository jpaInvestorRepository;
    private final ParticipationPersistenceMapper participationPersistenceMapper;

    public InvestorRepositoryAdapter(JpaInvestorRepository jpaInvestorRepository,
                                     ParticipationPersistenceMapper participationPersistenceMapper) {
        this.jpaInvestorRepository = jpaInvestorRepository;
        this.participationPersistenceMapper = participationPersistenceMapper;
    }

    @Override
    public Investor save(Investor investor) {
        return participationPersistenceMapper.toDomain(
                jpaInvestorRepository.save(participationPersistenceMapper.toEntity(investor))
        );
    }

    @Override
    public Optional<Investor> findById(Long investorId) {
        return jpaInvestorRepository.findById(investorId)
                .map(participationPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Investor> findByAccountId(Long accountId) {
        return jpaInvestorRepository.findByAccountId(accountId)
                .map(participationPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByAccountId(Long accountId) {
        return jpaInvestorRepository.existsByAccountId(accountId);
    }
}

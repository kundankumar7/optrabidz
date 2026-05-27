package com.project.optrabidz.participation.infrastructure.repository;

import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import com.project.optrabidz.participation.infrastructure.mapper.ParticipationPersistenceMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class StartupRepositoryAdapter implements StartupRepository {
    private final JpaStartupRepository jpaStartupRepository;
    private final ParticipationPersistenceMapper participationPersistenceMapper;

    public StartupRepositoryAdapter(JpaStartupRepository jpaStartupRepository,
                                    ParticipationPersistenceMapper participationPersistenceMapper) {
        this.jpaStartupRepository = jpaStartupRepository;
        this.participationPersistenceMapper = participationPersistenceMapper;
    }

    @Override
    public Startup save(Startup startup) {
        return participationPersistenceMapper.toDomain(
                jpaStartupRepository.save(participationPersistenceMapper.toEntity(startup))
        );
    }

    @Override
    public Optional<Startup> findById(Long startupId) {
        return jpaStartupRepository.findById(startupId)
                .map(participationPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Startup> findByAccountId(Long accountId) {
        return jpaStartupRepository.findByAccountId(accountId)
                .map(participationPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByAccountId(Long accountId) {
        return jpaStartupRepository.existsByAccountId(accountId);
    }
}

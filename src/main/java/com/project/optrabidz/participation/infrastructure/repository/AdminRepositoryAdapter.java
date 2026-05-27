package com.project.optrabidz.participation.infrastructure.repository;

import com.project.optrabidz.participation.domain.model.Admin;
import com.project.optrabidz.participation.domain.model.AdminState;
import com.project.optrabidz.participation.domain.repository.AdminRepository;
import com.project.optrabidz.participation.infrastructure.mapper.ParticipationPersistenceMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AdminRepositoryAdapter implements AdminRepository {
    private final JpaAdminRepository jpaAdminRepository;
    private final ParticipationPersistenceMapper participationPersistenceMapper;

    public AdminRepositoryAdapter(JpaAdminRepository jpaAdminRepository,
                                  ParticipationPersistenceMapper participationPersistenceMapper) {
        this.jpaAdminRepository = jpaAdminRepository;
        this.participationPersistenceMapper = participationPersistenceMapper;
    }

    @Override
    public Admin save(Admin admin) {
        return participationPersistenceMapper.toDomain(
                jpaAdminRepository.save(participationPersistenceMapper.toEntity(admin))
        );
    }

    @Override
    public Optional<Admin> findByAccountId(Long accountId) {
        return jpaAdminRepository.findByAccountId(accountId)
                .map(participationPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Admin> findActiveAdmin() {
        return jpaAdminRepository.findFirstByAdminState(AdminState.ACTIVE)
                .map(participationPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByAccountId(Long accountId) {
        return jpaAdminRepository.existsByAccountId(accountId);
    }

    @Override
    public boolean existsActiveAdmin() {
        return jpaAdminRepository.existsByAdminState(AdminState.ACTIVE);
    }

    @Override
    public boolean existsActiveByAccountId(Long accountId) {
        return jpaAdminRepository.existsByAccountIdAndAdminState(accountId, AdminState.ACTIVE);
    }
}

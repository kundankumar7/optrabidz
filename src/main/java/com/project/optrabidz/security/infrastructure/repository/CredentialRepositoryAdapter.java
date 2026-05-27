package com.project.optrabidz.security.infrastructure.repository;

import com.project.optrabidz.security.domain.model.Credential;
import com.project.optrabidz.security.domain.repository.CredentialRepository;
import com.project.optrabidz.security.infrastructure.mapper.SecurityPersistenceMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CredentialRepositoryAdapter implements CredentialRepository {
    private final JpaCredentialRepository jpaCredentialRepository;
    private final SecurityPersistenceMapper securityPersistenceMapper;

    public CredentialRepositoryAdapter(JpaCredentialRepository jpaCredentialRepository,
                                       SecurityPersistenceMapper securityPersistenceMapper) {
        this.jpaCredentialRepository = jpaCredentialRepository;
        this.securityPersistenceMapper = securityPersistenceMapper;
    }

    @Override
    public Credential save(Credential credential) {
        return securityPersistenceMapper.toDomain(
                jpaCredentialRepository.save(securityPersistenceMapper.toEntity(credential))
        );
    }

    @Override
    public Optional<Credential> findByEmail(String email) {
        return jpaCredentialRepository.findByEmailIgnoreCase(email)
                .map(securityPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Credential> findByAccountId(Long accountId) {
        return jpaCredentialRepository.findByAccountId(accountId)
                .map(securityPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaCredentialRepository.existsByEmailIgnoreCase(email);
    }
}

package com.project.optrabidz.security.infrastructure.repository;

import com.project.optrabidz.security.domain.model.LoginAttempt;
import com.project.optrabidz.security.domain.repository.LoginAttemptRepository;
import com.project.optrabidz.security.infrastructure.mapper.SecurityPersistenceMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LoginAttemptRepositoryAdapter implements LoginAttemptRepository {
    private final JpaLoginAttemptRepository jpaLoginAttemptRepository;
    private final SecurityPersistenceMapper securityPersistenceMapper;

    public LoginAttemptRepositoryAdapter(JpaLoginAttemptRepository jpaLoginAttemptRepository,
                                         SecurityPersistenceMapper securityPersistenceMapper) {
        this.jpaLoginAttemptRepository = jpaLoginAttemptRepository;
        this.securityPersistenceMapper = securityPersistenceMapper;
    }

    @Override
    public LoginAttempt save(LoginAttempt loginAttempt) {
        return securityPersistenceMapper.toDomain(
                jpaLoginAttemptRepository.save(securityPersistenceMapper.toEntity(loginAttempt))
        );
    }

    @Override
    public List<LoginAttempt> findRecentByEmail(String email, int limit) {
        return jpaLoginAttemptRepository.findByEmailIgnoreCaseOrderByAttemptedAtDesc(
                        email,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(securityPersistenceMapper::toDomain)
                .toList();
    }
}

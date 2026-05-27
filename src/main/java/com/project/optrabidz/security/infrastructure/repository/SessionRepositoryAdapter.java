package com.project.optrabidz.security.infrastructure.repository;

import com.project.optrabidz.security.domain.model.Session;
import com.project.optrabidz.security.domain.model.SessionStatus;
import com.project.optrabidz.security.domain.repository.SessionRepository;
import com.project.optrabidz.security.infrastructure.mapper.SecurityPersistenceMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class SessionRepositoryAdapter implements SessionRepository {
    private final JpaSessionRepository jpaSessionRepository;
    private final SecurityPersistenceMapper securityPersistenceMapper;

    public SessionRepositoryAdapter(JpaSessionRepository jpaSessionRepository,
                                    SecurityPersistenceMapper securityPersistenceMapper) {
        this.jpaSessionRepository = jpaSessionRepository;
        this.securityPersistenceMapper = securityPersistenceMapper;
    }

    @Override
    public Session save(Session session) {
        return securityPersistenceMapper.toDomain(
                jpaSessionRepository.save(securityPersistenceMapper.toEntity(session))
        );
    }

    @Override
    public Optional<Session> findById(Long sessionId) {
        return jpaSessionRepository.findById(sessionId)
                .map(securityPersistenceMapper::toDomain);
    }

    @Override
    public void expireExpiredSessions(Instant cutoff) {
        jpaSessionRepository.expireExpiredSessions(cutoff, SessionStatus.ACTIVE, SessionStatus.EXPIRED);
    }
}

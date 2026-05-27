package com.project.optrabidz.security.domain.repository;

import com.project.optrabidz.security.domain.model.Session;

import java.time.Instant;
import java.util.Optional;

public interface SessionRepository {
    Session save(Session session);

    Optional<Session> findById(Long sessionId);

    void expireExpiredSessions(Instant cutoff);
}

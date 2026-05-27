package com.project.optrabidz.security.infrastructure.repository;

import com.project.optrabidz.security.domain.model.SessionStatus;
import com.project.optrabidz.security.infrastructure.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface JpaSessionRepository extends JpaRepository<Session, Long> {
    @Modifying
    @Query("""
            update Session session
               set session.sessionStatus = :expiredStatus
             where session.sessionStatus = :activeStatus
               and session.expiresAt <= :cutoff
            """)
    void expireExpiredSessions(@Param("cutoff") Instant cutoff,
                               @Param("activeStatus") SessionStatus activeStatus,
                               @Param("expiredStatus") SessionStatus expiredStatus);
}

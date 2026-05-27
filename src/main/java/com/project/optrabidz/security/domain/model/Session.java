package com.project.optrabidz.security.domain.model;

import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;

public class Session {
    private Long sessionId;
    private Long accountId;
    private Instant createdAt;
    private Instant expiresAt;
    private SessionStatus sessionStatus;

    protected Session() {
    }

    public Session(Long sessionId,
                   Long accountId,
                   Instant createdAt,
                   Instant expiresAt,
                   SessionStatus sessionStatus) {
        this.sessionId = sessionId;
        this.accountId = accountId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.sessionStatus = sessionStatus;
    }

    public static Session start(Long accountId, Duration duration) {
        Assert.notNull(accountId, "accountId must not be null");
        Assert.notNull(duration, "duration must not be null");

        Instant now = Instant.now();
        return new Session(null, accountId, now, now.plus(duration), SessionStatus.ACTIVE);
    }

    public boolean isActive() {
        return sessionStatus == SessionStatus.ACTIVE;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public void terminate() {
        if (sessionStatus == SessionStatus.TERMINATED) {
            return;
        }
        if (sessionStatus == SessionStatus.EXPIRED) {
            throw new IllegalStateException("Expired session cannot be terminated");
        }
        this.sessionStatus = SessionStatus.TERMINATED;
    }

    public void expire() {
        if (sessionStatus == SessionStatus.EXPIRED) {
            return;
        }
        if (sessionStatus == SessionStatus.TERMINATED) {
            throw new IllegalStateException("Terminated session cannot expire");
        }
        this.sessionStatus = SessionStatus.EXPIRED;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public SessionStatus getSessionStatus() {
        return sessionStatus;
    }
}

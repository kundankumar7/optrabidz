package com.project.optrabidz.security.infrastructure.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "login_attempt")
public class LoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_attempt_id", nullable = false, updatable = false)
    private Long loginAttemptId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "source_ip")
    private String sourceIp;

    public LoginAttempt() {
    }

    public Long getLoginAttemptId() {
        return loginAttemptId;
    }

    public void setLoginAttemptId(Long loginAttemptId) {
        this.loginAttemptId = loginAttemptId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }
}

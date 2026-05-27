package com.project.optrabidz.security.domain.model;

import java.time.Instant;

public class LoginAttempt {
    private Long loginAttemptId;
    private String email;
    private Instant attemptedAt;
    private boolean success;
    private String failureReason;
    private String sourceIp;

    protected LoginAttempt() {
    }

    public LoginAttempt(Long loginAttemptId,
                        String email,
                        Instant attemptedAt,
                        boolean success,
                        String failureReason,
                        String sourceIp) {
        this.loginAttemptId = loginAttemptId;
        this.email = email;
        this.attemptedAt = attemptedAt;
        this.success = success;
        this.failureReason = failureReason;
        this.sourceIp = sourceIp;
    }

    public static LoginAttempt success(String email, String sourceIp) {
        return new LoginAttempt(null, email, Instant.now(), true, null, sourceIp);
    }

    public static LoginAttempt failure(String email, String failureReason, String sourceIp) {
        return new LoginAttempt(null, email, Instant.now(), false, failureReason, sourceIp);
    }

    public Long getLoginAttemptId() {
        return loginAttemptId;
    }

    public String getEmail() {
        return email;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getSourceIp() {
        return sourceIp;
    }
}

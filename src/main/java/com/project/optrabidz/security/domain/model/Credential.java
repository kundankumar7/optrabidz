package com.project.optrabidz.security.domain.model;

import org.springframework.util.Assert;

import java.time.Instant;

public class Credential {
    private Long credentialId;
    private Long accountId;
    private String email;
    private String passwordHash;
    private CredentialStatus credentialStatus;
    private Instant createdAt;
    private Instant passwordUpdatedAt;

    protected Credential() {
    }

    public Credential(Long credentialId,
                      Long accountId,
                      String email,
                      String passwordHash,
                      CredentialStatus credentialStatus,
                      Instant createdAt,
                      Instant passwordUpdatedAt) {
        this.credentialId = credentialId;
        this.accountId = accountId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.credentialStatus = credentialStatus;
        this.createdAt = createdAt;
        this.passwordUpdatedAt = passwordUpdatedAt;
    }

    public static Credential register(Long accountId, String email, String passwordHash) {
        Assert.notNull(accountId, "accountId must not be null");
        Assert.hasText(email, "email must not be blank");
        Assert.hasText(passwordHash, "passwordHash must not be blank");

        return new Credential(
                null,
                accountId,
                email,
                passwordHash,
                CredentialStatus.ACTIVE,
                Instant.now(),
                null
        );
    }

    public boolean canAuthenticate() {
        return credentialStatus == CredentialStatus.ACTIVE;
    }

    public void changePassword(String newPasswordHash) {
        Assert.hasText(newPasswordHash, "newPasswordHash must not be blank");
        ensureNotDisabled();
        this.passwordHash = newPasswordHash;
        this.passwordUpdatedAt = Instant.now();
    }

    public void lock() {
        if (credentialStatus == CredentialStatus.DISABLED) {
            throw new IllegalStateException("Disabled credential cannot be locked");
        }
        if (credentialStatus == CredentialStatus.LOCKED) {
            return;
        }
        this.credentialStatus = CredentialStatus.LOCKED;
    }

    public void unlock() {
        if (credentialStatus != CredentialStatus.LOCKED) {
            throw new IllegalStateException("Only locked credential can be unlocked");
        }
        this.credentialStatus = CredentialStatus.ACTIVE;
    }

    public void disable() {
        if (credentialStatus == CredentialStatus.DISABLED) {
            return;
        }
        this.credentialStatus = CredentialStatus.DISABLED;
    }

    private void ensureNotDisabled() {
        if (credentialStatus == CredentialStatus.DISABLED) {
            throw new IllegalStateException("Disabled credential cannot be modified");
        }
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public CredentialStatus getCredentialStatus() {
        return credentialStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPasswordUpdatedAt() {
        return passwordUpdatedAt;
    }
}

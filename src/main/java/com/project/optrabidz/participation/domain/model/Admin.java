package com.project.optrabidz.participation.domain.model;

import org.springframework.util.Assert;

import java.time.Instant;

public class Admin {
    private Long adminId;
    private Long accountId;
    private String publicDisplayName;
    private String organizationLabel;
    private AdminState adminState;
    private Instant grantedAt;
    private Instant revokedAt;
    private Long revokedByAccountId;
    private String revokedReason;

    protected Admin() {
    }

    public Admin(Long adminId,
                 Long accountId,
                 String publicDisplayName,
                 String organizationLabel,
                 AdminState adminState,
                 Instant grantedAt,
                 Instant revokedAt,
                 Long revokedByAccountId,
                 String revokedReason) {
        this.adminId = adminId;
        this.accountId = accountId;
        this.publicDisplayName = publicDisplayName;
        this.organizationLabel = organizationLabel;
        this.adminState = adminState;
        this.grantedAt = grantedAt;
        this.revokedAt = revokedAt;
        this.revokedByAccountId = revokedByAccountId;
        this.revokedReason = revokedReason;
    }

    public static Admin grant(Long accountId, String publicDisplayName, String organizationLabel) {
        Assert.notNull(accountId, "accountId must not be null");
        Assert.hasText(publicDisplayName, "publicDisplayName must not be blank");

        return new Admin(
                null,
                accountId,
                publicDisplayName.trim(),
                normalizeOptionalText(organizationLabel),
                AdminState.ACTIVE,
                Instant.now(),
                null,
                null,
                null
        );
    }

    public void revoke(Long revokedByAccountId, String reason) {
        Assert.hasText(reason, "revocation reason must not be blank");

        if (adminState == AdminState.REVOKED) {
            return;
        }

        this.adminState = AdminState.REVOKED;
        this.revokedAt = Instant.now();
        this.revokedByAccountId = revokedByAccountId;
        this.revokedReason = reason.trim();
    }

    public boolean isActive() {
        return adminState == AdminState.ACTIVE;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Long getAdminId() {
        return adminId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getPublicDisplayName() {
        return publicDisplayName;
    }

    public String getOrganizationLabel() {
        return organizationLabel;
    }

    public AdminState getAdminState() {
        return adminState;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Long getRevokedByAccountId() {
        return revokedByAccountId;
    }

    public String getRevokedReason() {
        return revokedReason;
    }
}

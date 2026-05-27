package com.project.optrabidz.participation.infrastructure.entity;

import com.project.optrabidz.participation.domain.model.AdminState;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "admin")
public class AdminEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id", nullable = false, updatable = false)
    private Long adminId;

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "public_display_name", nullable = false)
    private String publicDisplayName;

    @Column(name = "organization_label")
    private String organizationLabel;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "admin_state", nullable = false, columnDefinition = "admin_state_enum")
    private AdminState adminState;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by_account_id")
    private Long revokedByAccountId;

    @Column(name = "revoked_reason")
    private String revokedReason;

    public AdminEntity() {
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getPublicDisplayName() {
        return publicDisplayName;
    }

    public void setPublicDisplayName(String publicDisplayName) {
        this.publicDisplayName = publicDisplayName;
    }

    public String getOrganizationLabel() {
        return organizationLabel;
    }

    public void setOrganizationLabel(String organizationLabel) {
        this.organizationLabel = organizationLabel;
    }

    public AdminState getAdminState() {
        return adminState;
    }

    public void setAdminState(AdminState adminState) {
        this.adminState = adminState;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Long getRevokedByAccountId() {
        return revokedByAccountId;
    }

    public void setRevokedByAccountId(Long revokedByAccountId) {
        this.revokedByAccountId = revokedByAccountId;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public void setRevokedReason(String revokedReason) {
        this.revokedReason = revokedReason;
    }
}

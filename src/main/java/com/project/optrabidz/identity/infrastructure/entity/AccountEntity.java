package com.project.optrabidz.identity.infrastructure.entity;

import com.project.optrabidz.identity.domain.model.AccountState;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "account")
@NamedEntityGraph(
        name = "Account.withRoleAndProfile",
        attributeNodes = {
                @NamedAttributeNode("role"),
                @NamedAttributeNode("profile")
        }
)
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "account_state", nullable = false, columnDefinition = "account_state_enum")
    private AccountState accountState;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private RoleEntity role;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private ProfileEntity profile;

    public AccountEntity() {
    }

    public AccountEntity(Long accountId,
                         AccountState accountState,
                         Instant createdAt,
                         Instant deactivatedAt,
                         RoleEntity role,
                         ProfileEntity profile) {
        this.accountId = accountId;
        this.accountState = accountState;
        this.createdAt = createdAt;
        this.deactivatedAt = deactivatedAt;
        this.role = role;
        this.profile = profile;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public AccountState getAccountState() {
        return accountState;
    }

    public void setAccountState(AccountState accountState) {
        this.accountState = accountState;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(Instant deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public ProfileEntity getProfile() {
        return profile;
    }

    public void setProfile(ProfileEntity profile) {
        this.profile = profile;
    }
}

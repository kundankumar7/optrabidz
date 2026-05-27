package com.project.optrabidz.identity.infrastructure.entity;

import com.project.optrabidz.identity.domain.model.RoleType;
import jakarta.persistence.*;

@Entity
@Table(name = "role",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_account", columnNames = "account_id"))
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", nullable = false, updatable = false)
    private Long roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private RoleType roleType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private AccountEntity account;

    public RoleEntity() {
    }

    public RoleEntity(Long roleId, RoleType roleType, AccountEntity account) {
        this.roleId = roleId;
        this.roleType = roleType;
        this.account = account;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public AccountEntity getAccount() {
        return account;
    }

    public void setAccount(AccountEntity account) {
        this.account = account;
    }
}

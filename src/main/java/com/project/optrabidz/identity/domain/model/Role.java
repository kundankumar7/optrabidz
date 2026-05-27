package com.project.optrabidz.identity.domain.model;

import org.springframework.util.Assert;

public class Role {
    private Long roleId;
    private Long accountId;
    private RoleType roleType;

    protected Role() {}

    public Role(RoleType roleType) {
        Assert.notNull(roleType, "RoleType must not be null");
        this.roleType = roleType;
    }

    // For mapper
    public Role(Long roleId, Long accountId, RoleType roleType) {
        this.roleId = roleId;
        this.accountId = accountId;
        this.roleType = roleType;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public Long getRoleId() {
        return roleId;
    }

    public Long getAccountId() {
        return accountId;
    }
}

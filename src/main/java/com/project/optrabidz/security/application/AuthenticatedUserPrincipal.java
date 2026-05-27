package com.project.optrabidz.security.application;

import com.project.optrabidz.identity.domain.model.RoleType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public final class AuthenticatedUserPrincipal implements Serializable {
    private final Long accountId;
    private final String email;
    private final RoleType role;

    public AuthenticatedUserPrincipal(Long accountId, String email, RoleType role) {
        this.accountId = accountId;
        this.email = email;
        this.role = role;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getEmail() {
        return email;
    }

    public RoleType getRole() {
        return role;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}

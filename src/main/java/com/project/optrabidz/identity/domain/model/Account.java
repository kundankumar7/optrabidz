package com.project.optrabidz.identity.domain.model;

import org.springframework.util.Assert;

import java.time.Instant;

public class Account {
    private Long accountId;
    private AccountState accountState;
    private Instant createdAt;
    private Instant deactivatedAt;

    private Role role;
    private Profile profile;

    // Constructor for new Account (no ID yet)
//    public Account(AccountState accountState, Instant createdAt) {
//        this.accountId = null;
//        this.accountState = accountState;
//        this.createdAt = createdAt;
//        this.deactivatedAt = null;
//    }

    protected Account() {}

    public Account(Long accountId, AccountState accountState, Instant createdAt, Instant deactivatedAt) {
        this.accountId = accountId;
        this.accountState = accountState;
        this.createdAt = createdAt;
        this.deactivatedAt = deactivatedAt;
    }

    // Constructor for existing Account (from DB)
//    public Account(Long accountId,
//                   AccountState accountState,
//                   Instant createdAt,
//                   Instant deactivatedAt) {
//        this.accountId = accountId;
//        this.accountState = accountState;
//        this.createdAt = createdAt;
//        this.deactivatedAt = deactivatedAt;
//    }

    // =====================
    // Behavior (State Machine)
    // =====================

    // =========================
    // FACTORY METHOD
    // =========================
    public static Account register() {
        Account account = new Account();
        account.accountState = AccountState.CREATED;
        account.createdAt = Instant.now();
        return account;
    }

    // =========================
    // BEHAVIOR METHODS
    // =========================

    public void assignRole(Role role) {
        Assert.notNull(role, "Role must not be null");

        if (this.role != null) {
            throw new IllegalStateException("Role already assigned");
        }

        this.role = role;
    }

    public void establishProfile(Profile profile) {
        Assert.notNull(profile, "Profile must not be null");

        if (this.profile != null) {
            throw new IllegalStateException("Profile already exists");
        }

        this.profile = profile;
    }

    public void enable() {
        if (this.accountState != AccountState.CREATED) {
            throw new IllegalStateException("Only CREATED account can be enabled");
        }
        this.accountState = AccountState.ACTIVE;
    }

    public void suspend() {
        if (this.accountState != AccountState.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE account can be suspended");
        }
        this.accountState = AccountState.SUSPENDED;
    }

    public void reinstate() {
        if (this.accountState != AccountState.SUSPENDED) {
            throw new IllegalStateException("Only SUSPENDED account can be reinstated");
        }
        this.accountState = AccountState.ACTIVE;
    }

    public void deactivate() {
        if (this.accountState == AccountState.DEACTIVATED) {
            throw new IllegalStateException("Already deactivated");
        }

        this.accountState = AccountState.DEACTIVATED;
        this.deactivatedAt = Instant.now();
    }

    // =========================
    // INTERNAL (FOR MAPPER ONLY)
    // =========================

    public void attachRole(Role role) {
        this.role = role;
    }

    public void attachProfile(Profile profile) {
        this.profile = profile;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    // =========================
    // GETTERS
    // =========================

    public Long getAccountId() {
        return accountId;
    }

    public AccountState getAccountState() {
        return accountState;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public Role getRole() {
        return role;
    }

    public Profile getProfile() {
        return profile;
    }
}

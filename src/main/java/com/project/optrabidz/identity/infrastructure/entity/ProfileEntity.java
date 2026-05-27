package com.project.optrabidz.identity.infrastructure.entity;

import com.project.optrabidz.identity.domain.model.ProfileStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "profile",
        uniqueConstraints = @UniqueConstraint(name = "uk_profile_account", columnNames = "account_id"))
public class ProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id", nullable = false, updatable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status", nullable = false)
    private ProfileStatus profileStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private AccountEntity account;

    public ProfileEntity() {
    }

    public ProfileEntity(Long profileId, ProfileStatus profileStatus, AccountEntity account) {
        this.profileId = profileId;
        this.profileStatus = profileStatus;
        this.account = account;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public ProfileStatus getProfileStatus() {
        return profileStatus;
    }

    public void setProfileStatus(ProfileStatus profileStatus) {
        this.profileStatus = profileStatus;
    }

    public AccountEntity getAccount() {
        return account;
    }

    public void setAccount(AccountEntity account) {
        this.account = account;
    }
}

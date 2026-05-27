package com.project.optrabidz.identity.domain.model;

import org.springframework.util.Assert;

public class Profile {
    private Long profileId;
    private Long accountId;
    private ProfileStatus profileStatus;

    protected Profile() {}

    // New profile
    public Profile(ProfileStatus profileStatus) {
        Assert.notNull(profileStatus, "ProfileStatus must not be null");
        this.profileStatus = profileStatus;
    }

    // For mapper
    public Profile(Long profileId, Long accountId, ProfileStatus profileStatus) {
        this.profileId = profileId;
        this.accountId = accountId;
        this.profileStatus = profileStatus;
    }


    public void markComplete() {
        if (this.profileStatus == ProfileStatus.COMPLETE) {
            throw new IllegalStateException("Profile already complete");
        }
        this.profileStatus = ProfileStatus.COMPLETE;
    }

    public void updateStatus(ProfileStatus profileStatus) {
        Assert.notNull(profileStatus, "profileStatus must not be null");
        this.profileStatus = profileStatus;
    }

    public ProfileStatus getProfileStatus() {
        return profileStatus;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Long getAccountId() {
        return accountId;
    }
}

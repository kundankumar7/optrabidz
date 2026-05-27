package com.project.optrabidz.identity.application.query;

import com.project.optrabidz.identity.domain.model.Account;
import com.project.optrabidz.identity.domain.model.AccountState;
import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.RoleType;

public record AccountSnapshot(
        Long accountId,
        AccountState accountState,
        ProfileStatus profileStatus,
        RoleType roleType
) {
    public static AccountSnapshot from(Account account) {
        return new AccountSnapshot(
                account.getAccountId(),
                account.getAccountState(),
                account.getProfile().getProfileStatus(),
                account.getRole().getRoleType()
        );
    }
}

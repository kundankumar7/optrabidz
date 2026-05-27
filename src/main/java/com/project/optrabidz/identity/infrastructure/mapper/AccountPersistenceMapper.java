package com.project.optrabidz.identity.infrastructure.mapper;

import com.project.optrabidz.identity.domain.model.Account;
import com.project.optrabidz.identity.domain.model.Profile;
import com.project.optrabidz.identity.domain.model.Role;
import com.project.optrabidz.identity.infrastructure.entity.AccountEntity;
import com.project.optrabidz.identity.infrastructure.entity.ProfileEntity;
import com.project.optrabidz.identity.infrastructure.entity.RoleEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountPersistenceMapper {
    public AccountEntity toEntity(Account account) {

        // Account
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setAccountId(account.getAccountId());
        accountEntity.setAccountState(account.getAccountState());
        accountEntity.setCreatedAt(account.getCreatedAt());
        accountEntity.setDeactivatedAt(account.getDeactivatedAt());

        // Role
        Role role = account.getRole();
        if (role == null) {
            throw new IllegalStateException("Role must not be null");
        }

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRoleId(role.getRoleId());
        roleEntity.setRoleType(role.getRoleType());
        roleEntity.setAccount(accountEntity);

        // Profile
        Profile profile = account.getProfile();
        if (profile == null) {
            throw new IllegalStateException("Profile must not be null");
        }

        ProfileEntity profileEntity = new ProfileEntity();
        profileEntity.setProfileId(profile.getProfileId());
        profileEntity.setProfileStatus(profile.getProfileStatus());
        profileEntity.setAccount(accountEntity);

        // Set relationships
        accountEntity.setRole(roleEntity);
        accountEntity.setProfile(profileEntity);

        return accountEntity;
    }

    public Account toDomain(AccountEntity entity) {

        if (entity.getRole() == null || entity.getProfile() == null) {
            throw new IllegalStateException("Incomplete aggregate: Role or Profile missing");
        }

        // Account
        Account account = new Account(
                entity.getAccountId(),
                entity.getAccountState(),
                entity.getCreatedAt(),
                entity.getDeactivatedAt()
        );

        // Role
        RoleEntity roleEntity = entity.getRole();
        Role role = new Role(
                roleEntity.getRoleId(),
                entity.getAccountId(),
                roleEntity.getRoleType()
        );

        // Profile
        ProfileEntity profileEntity = entity.getProfile();
        Profile profile = new Profile(
                profileEntity.getProfileId(),
                entity.getAccountId(),
                profileEntity.getProfileStatus()
        );

        // Attach to aggregate
        account.attachRole(role);
        account.attachProfile(profile);

        return account;
    }
}

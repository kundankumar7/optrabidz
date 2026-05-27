package com.project.optrabidz.identity.application.service;

import com.project.optrabidz.common.event.AccountRegisteredEvent;
import com.project.optrabidz.identity.application.command.ActivateAccountCommand;
import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.identity.application.command.CreateAccountCommand;
import com.project.optrabidz.identity.application.command.DeactivateAccountCommand;
import com.project.optrabidz.identity.application.command.UpdateProfileStatusCommand;
import com.project.optrabidz.identity.application.exception.InvalidAccountStateException;
import com.project.optrabidz.identity.application.port.IdentityCommandPort;
import com.project.optrabidz.identity.domain.model.Account;
import com.project.optrabidz.identity.domain.model.Profile;
import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.Role;
import com.project.optrabidz.identity.domain.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Instant;

@Service
public class AccountApplicationService implements IdentityCommandPort {
    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;

    public AccountApplicationService(AccountRepository accountRepository,
                                     EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Long createAccount(CreateAccountCommand command) {
        Assert.notNull(command, "CreateAccountCommand must not be null");
        Assert.notNull(command.roleType(), "RoleType must not be null");

        Account account = Account.register();
        account.assignRole(new Role(command.roleType()));
        account.establishProfile(new Profile(ProfileStatus.INCOMPLETE));

        Account savedAccount = accountRepository.save(account);
        eventPublisher.publish(new AccountRegisteredEvent(
                savedAccount.getAccountId(),
                savedAccount.getRole().getRoleType(),
                Instant.now()
        ));

        return savedAccount.getAccountId();
    }

    @Override
    @Transactional
    public void activateAccount(ActivateAccountCommand command) {
        Assert.notNull(command, "ActivateAccountCommand must not be null");
        activateAccount(command.accountId());
    }

    @Override
    @Transactional
    public void deactivateAccount(DeactivateAccountCommand command) {
        Assert.notNull(command, "DeactivateAccountCommand must not be null");
        deactivateAccount(command.accountId());
    }

    @Override
    @Transactional
    public void updateProfileStatus(UpdateProfileStatusCommand command) {
        Assert.notNull(command, "UpdateProfileStatusCommand must not be null");
        Assert.notNull(command.accountId(), "accountId must not be null");
        Assert.notNull(command.profileStatus(), "profileStatus must not be null");

        Account account = requireAccount(command.accountId());
        if (account.getProfile().getProfileStatus() == command.profileStatus()) {
            return;
        }

        account.getProfile().updateStatus(command.profileStatus());
        accountRepository.save(account);
    }

    @Transactional
    public Account activateAccount(Long accountId) {
        return updateAccountState(accountId, Account::enable, "activate");
    }

    @Transactional
    public Account suspendAccount(Long accountId) {
        return updateAccountState(accountId, Account::suspend, "suspend");
    }

    @Transactional
    public Account reinstateAccount(Long accountId) {
        return updateAccountState(accountId, Account::reinstate, "reinstate");
    }

    @Transactional
    public Account deactivateAccount(Long accountId) {
        return updateAccountState(accountId, Account::deactivate, "deactivate");
    }

    @Transactional
    public Account completeProfile(Long accountId) {
        Account account = requireAccount(accountId);

        try {
            account.getProfile().markComplete();
        } catch (IllegalStateException exception) {
            throw new InvalidAccountStateException(
                    "Unable to complete profile for account " + accountId + ": " + exception.getMessage(),
                    exception
            );
        }

        return accountRepository.save(account);
    }

    private Account updateAccountState(Long accountId, AccountMutation mutation, String action) {
        Account account = requireAccount(accountId);

        try {
            mutation.apply(account);
        } catch (IllegalStateException exception) {
            throw new InvalidAccountStateException(
                    "Unable to " + action + " account " + accountId + ": " + exception.getMessage(),
                    exception
            );
        }

        return accountRepository.save(account);
    }

    private Account requireAccount(Long accountId) {
        Assert.notNull(accountId, "accountId must not be null");
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }

    @FunctionalInterface
    private interface AccountMutation {
        void apply(Account account);
    }
}

package com.project.optrabidz.identity.application.service;

import com.project.optrabidz.common.event.AccountRegisteredEvent;
import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.identity.application.command.CreateAccountCommand;
import com.project.optrabidz.identity.application.command.UpdateProfileStatusCommand;
import com.project.optrabidz.identity.application.error.IdentityErrors;
import com.project.optrabidz.identity.application.exception.AccountNotFoundException;
import com.project.optrabidz.identity.application.exception.AccountStateConflictException;
import com.project.optrabidz.identity.application.exception.ProfileStateConflictException;
import com.project.optrabidz.identity.domain.model.Account;
import com.project.optrabidz.identity.domain.model.AccountState;
import com.project.optrabidz.identity.domain.model.Profile;
import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.Role;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.identity.domain.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountApplicationServiceTest {

    private static final Long ACCOUNT_ID = 41L;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EventPublisher eventPublisher;

    private AccountApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AccountApplicationService(accountRepository, eventPublisher);
    }

    @Test
    void activateAccountTranslatesMissingAccountWithoutSaving() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateAccount(ACCOUNT_ID))
                .isInstanceOfSatisfying(AccountNotFoundException.class, failure -> {
                    assertThat(failure.descriptor()).isSameAs(IdentityErrors.ACCOUNT_NOT_FOUND);
                    assertThat(failure.getMessage()).contains(ACCOUNT_ID.toString());
                });
        verify(accountRepository, never()).save(any());
    }

    @Test
    void activateAccountTranslatesDomainStateConflictAndRetainsCause() {
        Account activeAccount = account(AccountState.ACTIVE, ProfileStatus.INCOMPLETE);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(activeAccount));

        assertThatThrownBy(() -> service.activateAccount(ACCOUNT_ID))
                .isInstanceOfSatisfying(AccountStateConflictException.class, failure -> {
                    assertThat(failure.descriptor()).isSameAs(IdentityErrors.ACCOUNT_STATE_CONFLICT);
                    assertThat(failure.getCause()).isInstanceOf(IllegalStateException.class);
                    assertThat(failure.getMessage()).contains("activate", ACCOUNT_ID.toString());
                });
        verify(accountRepository, never()).save(any());
    }

    @Test
    void completeProfileTranslatesDomainStateConflictAndRetainsCause() {
        Account completedAccount = account(AccountState.ACTIVE, ProfileStatus.COMPLETE);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(completedAccount));

        assertThatThrownBy(() -> service.completeProfile(ACCOUNT_ID))
                .isInstanceOfSatisfying(ProfileStateConflictException.class, failure -> {
                    assertThat(failure.descriptor()).isSameAs(IdentityErrors.PROFILE_STATE_CONFLICT);
                    assertThat(failure.getCause()).isInstanceOf(IllegalStateException.class);
                    assertThat(failure.getMessage()).contains("complete", ACCOUNT_ID.toString());
                });
        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateProfileStatusRemainsIdempotentWhenStatusIsUnchanged() {
        Account account = account(AccountState.ACTIVE, ProfileStatus.COMPLETE);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        service.updateProfileStatus(new UpdateProfileStatusCommand(ACCOUNT_ID, ProfileStatus.COMPLETE));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void validAccountTransitionPersistsAndReturnsTheAggregate() {
        Account createdAccount = account(AccountState.CREATED, ProfileStatus.INCOMPLETE);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(createdAccount));
        when(accountRepository.save(createdAccount)).thenReturn(createdAccount);

        Account result = service.activateAccount(ACCOUNT_ID);

        assertThat(result).isSameAs(createdAccount);
        assertThat(result.getAccountState()).isEqualTo(AccountState.ACTIVE);
        verify(accountRepository).save(createdAccount);
    }

    @Test
    void accountCreationPublishesTheExistingRegistrationEvent() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setAccountId(ACCOUNT_ID);
            return account;
        });

        Long accountId = service.createAccount(new CreateAccountCommand(RoleType.STARTUP));

        assertThat(accountId).isEqualTo(ACCOUNT_ID);
        ArgumentCaptor<AccountRegisteredEvent> eventCaptor =
                ArgumentCaptor.forClass(AccountRegisteredEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(eventCaptor.getValue().roleType()).isEqualTo(RoleType.STARTUP);
        assertThat(eventCaptor.getValue().occurredAt()).isNotNull();
    }

    private static Account account(AccountState state, ProfileStatus profileStatus) {
        Account account = new Account(ACCOUNT_ID, state, Instant.parse("2026-08-21T00:00:00Z"), null);
        account.attachRole(new Role(RoleType.STARTUP));
        account.attachProfile(new Profile(profileStatus));
        return account;
    }
}

package com.project.optrabidz.security.application;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.identity.application.port.IdentityQueryPort;
import com.project.optrabidz.identity.application.query.AccountSnapshot;
import com.project.optrabidz.identity.domain.model.AccountState;
import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.domain.repository.AdminRepository;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeServiceTest {

    private static final Long ACCOUNT_ID = 41L;

    @Mock
    private IdentityQueryPort identityQueryPort;
    @Mock
    private StartupRepository startupRepository;
    @Mock
    private InvestorRepository investorRepository;
    @Mock
    private AdminRepository adminRepository;

    private MeService service;
    private AuthenticatedUserPrincipal principal;

    @BeforeEach
    void setUp() {
        service = new MeService(
                identityQueryPort, startupRepository, investorRepository, adminRepository);
        principal = new AuthenticatedUserPrincipal(
                ACCOUNT_ID, "member@example.com", RoleType.STARTUP);
    }

    @Test
    void missingAuthenticatedAccountIsInternalConsistencyFailure() {
        when(identityQueryPort.findAccountById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUser(principal))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(ApplicationException.class)
                .hasMessage("Authenticated session references a missing account");
    }

    @Test
    void currentUserResponseKeepsRoleStateProfileAndActorPresence() {
        when(identityQueryPort.findAccountById(ACCOUNT_ID)).thenReturn(Optional.of(
                new AccountSnapshot(
                        ACCOUNT_ID, AccountState.ACTIVE, ProfileStatus.COMPLETE, RoleType.STARTUP)));
        when(startupRepository.existsByAccountId(ACCOUNT_ID)).thenReturn(true);

        var response = service.getCurrentUser(principal);

        assertThat(response.role()).isEqualTo(RoleType.STARTUP);
        assertThat(response.accountState()).isEqualTo(AccountState.ACTIVE);
        assertThat(response.profileStatus()).isEqualTo(ProfileStatus.COMPLETE);
        assertThat(response.actorType()).isEqualTo(RoleType.STARTUP);
        assertThat(response.actorExists()).isTrue();
    }
}

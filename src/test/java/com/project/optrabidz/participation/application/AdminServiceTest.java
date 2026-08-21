package com.project.optrabidz.participation.application;

import com.project.optrabidz.participation.application.exception.ActiveAdminAlreadyExistsException;
import com.project.optrabidz.participation.application.exception.ActiveAdminNotFoundException;
import com.project.optrabidz.participation.application.exception.AdminAuthorityAlreadyGrantedException;
import com.project.optrabidz.participation.domain.model.Admin;
import com.project.optrabidz.participation.domain.model.AdminState;
import com.project.optrabidz.participation.domain.repository.AdminRepository;
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
class AdminServiceTest {

    private static final Long ACCOUNT_ID = 43L;

    @Mock
    private AdminRepository adminRepository;

    private AdminService service;

    @BeforeEach
    void setUp() {
        service = new AdminService(adminRepository);
    }

    @Test
    void activeAdministratorConflictTakesPrecedenceOverAccountHistory() {
        when(adminRepository.existsActiveAdmin()).thenReturn(true);

        assertThatThrownBy(() -> service.createActiveAdmin(
                ACCOUNT_ID, "Admin", "OptraBidz"))
                .isInstanceOf(ActiveAdminAlreadyExistsException.class);

        verify(adminRepository, never()).existsByAccountId(ACCOUNT_ID);
        verify(adminRepository, never()).save(any());
    }

    @Test
    void historicalAuthorityConflictIsUsedOnlyWhenNoAdminIsActive() {
        when(adminRepository.existsActiveAdmin()).thenReturn(false);
        when(adminRepository.existsByAccountId(ACCOUNT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createActiveAdmin(
                ACCOUNT_ID, "Admin", "OptraBidz"))
                .isInstanceOf(AdminAuthorityAlreadyGrantedException.class);

        verify(adminRepository, never()).save(any());
    }

    @Test
    void revokeWithoutActiveAdministratorUsesSpecificNotFoundFailure() {
        when(adminRepository.findActiveAdmin()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revokeActiveAdmin(99L, "Transfer"))
                .isInstanceOf(ActiveAdminNotFoundException.class);

        verify(adminRepository, never()).save(any());
    }

    @Test
    void successfulGrantPreservesActiveAdminStateAndPersistence() {
        when(adminRepository.existsActiveAdmin()).thenReturn(false);
        when(adminRepository.existsByAccountId(ACCOUNT_ID)).thenReturn(false);
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Admin result = service.createActiveAdmin(ACCOUNT_ID, " Admin ", " OptraBidz ");

        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getAdminState()).isEqualTo(AdminState.ACTIVE);
        assertThat(result.getPublicDisplayName()).isEqualTo("Admin");
        assertThat(result.getOrganizationLabel()).isEqualTo("OptraBidz");
    }

    @Test
    void successfulRevokePreservesDomainTransitionAndPersistence() {
        Admin active = new Admin(
                7L, ACCOUNT_ID, "Admin", "OptraBidz", AdminState.ACTIVE,
                Instant.parse("2026-08-21T00:00:00Z"), null, null, null);
        when(adminRepository.findActiveAdmin()).thenReturn(Optional.of(active));
        when(adminRepository.save(active)).thenReturn(active);

        Admin result = service.revokeActiveAdmin(99L, " Governance transfer ");

        assertThat(result.getAdminState()).isEqualTo(AdminState.REVOKED);
        assertThat(result.getRevokedByAccountId()).isEqualTo(99L);
        assertThat(result.getRevokedReason()).isEqualTo("Governance transfer");
        assertThat(result.getRevokedAt()).isNotNull();
        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(active);
    }
}

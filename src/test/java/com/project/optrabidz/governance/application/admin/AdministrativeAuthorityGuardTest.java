package com.project.optrabidz.governance.application.admin;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.governance.application.admin.exception.AdminAuthorityUnavailableException;
import com.project.optrabidz.governance.application.admin.exception.AdminRecoveryAccessDeniedException;
import com.project.optrabidz.governance.application.common.GovernanceException;
import com.project.optrabidz.governance.application.error.GovernanceErrors;
import com.project.optrabidz.participation.application.port.AdminAuthorityQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministrativeAuthorityGuardTest {

    private static final Long ACCOUNT_ID = 47L;

    @Mock
    private AdminAuthorityQueryPort adminAuthorityQueryPort;

    private AdministrativeAuthorityGuard guard;

    @BeforeEach
    void setUp() {
        guard = new AdministrativeAuthorityGuard(adminAuthorityQueryPort);
    }

    @Test
    void disabledRecoveryModeStopsTransferBeforeAuthorityLookup() {
        ApplicationException failure = failureFrom(
                () -> guard.assertRecoveryTransferAllowed(false),
                AdminRecoveryAccessDeniedException.class
        );

        assertThat(failure.descriptor())
                .isSameAs(GovernanceErrors.ADMIN_RECOVERY_ACCESS_DENIED);
        assertThat(failure.diagnosticCode())
                .isEqualTo("GOVERNANCE.RECOVERY.MODE_DISABLED");
        verify(adminAuthorityQueryPort, never()).activeAdminExists();
    }

    @Test
    void enabledRecoveryWithoutActiveAuthorityUsesStateConflict() {
        when(adminAuthorityQueryPort.activeAdminExists()).thenReturn(false);

        ApplicationException failure = failureFrom(
                () -> guard.assertRecoveryTransferAllowed(true),
                AdminAuthorityUnavailableException.class
        );

        assertThat(failure.descriptor())
                .isSameAs(GovernanceErrors.ADMIN_AUTHORITY_UNAVAILABLE);
        assertThat(failure.diagnosticCode())
                .isEqualTo("GOVERNANCE.ADMIN_AUTHORITY.UNAVAILABLE");
    }

    @Test
    void enabledRecoveryWithActiveAuthorityAllowsTransfer() {
        when(adminAuthorityQueryPort.activeAdminExists()).thenReturn(true);

        assertThatCode(() -> guard.assertRecoveryTransferAllowed(true))
                .doesNotThrowAnyException();
    }

    @Test
    void inactiveAdminAccountUsesGovernancePermissionFailure() {
        when(adminAuthorityQueryPort.isActiveAdmin(ACCOUNT_ID)).thenReturn(false);

        ApplicationException failure = failureFrom(
                () -> guard.assertActiveAdmin(ACCOUNT_ID),
                GovernanceException.class
        );

        assertThat(failure.descriptor())
                .isSameAs(GovernanceErrors.GOVERNANCE_ACTION_NOT_PERMITTED);
        assertThat(failure.diagnosticCode())
                .isEqualTo("GOVERNANCE.ADMIN_AUTHORITY_REQUIRED");
        assertThat(failure.getMessage()).doesNotContain(ACCOUNT_ID.toString());
    }

    @Test
    void activeAdminAccountPassesAuthorityGuard() {
        when(adminAuthorityQueryPort.isActiveAdmin(ACCOUNT_ID)).thenReturn(true);

        assertThatCode(() -> guard.assertActiveAdmin(ACCOUNT_ID))
                .doesNotThrowAnyException();
    }

    private static ApplicationException failureFrom(
            Runnable operation,
            Class<? extends ApplicationException> expectedType
    ) {
        Throwable failure = catchThrowable(operation::run);
        assertThat(failure).isInstanceOf(expectedType);
        return (ApplicationException) failure;
    }
}

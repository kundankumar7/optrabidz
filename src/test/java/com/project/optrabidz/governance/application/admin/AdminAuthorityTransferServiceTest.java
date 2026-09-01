package com.project.optrabidz.governance.application.admin;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.governance.application.admin.exception.AdminAuthorityUnavailableException;
import com.project.optrabidz.identity.application.port.IdentityCommandPort;
import com.project.optrabidz.participation.application.port.AdminProvisioningPort;
import com.project.optrabidz.security.application.port.SecurityCredentialProvisioningPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminAuthorityTransferServiceTest {

    @Mock
    private AdministrativeAuthorityGuard authorityGuard;
    @Mock
    private AdminProvisioningPort adminProvisioningPort;
    @Mock
    private SecurityCredentialProvisioningPort credentialProvisioningPort;
    @Mock
    private IdentityCommandPort identityCommandPort;
    @Mock
    private AdminBootstrapService adminBootstrapService;
    @Mock
    private EventPublisher eventPublisher;

    private AdminAuthorityTransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new AdminAuthorityTransferService(
                authorityGuard,
                adminProvisioningPort,
                credentialProvisioningPort,
                identityCommandPort,
                adminBootstrapService,
                eventPublisher
        );
    }

    @Test
    void unavailableAuthorityStopsBeforeEveryDownstreamMutationAndEvent() {
        doThrow(new AdminAuthorityUnavailableException())
                .when(authorityGuard)
                .assertRecoveryTransferAllowed(true);

        assertThatThrownBy(() -> transferService.transferAuthority(
                validCommand(),
                true
        )).isInstanceOf(AdminAuthorityUnavailableException.class);

        verifyNoInteractions(
                adminProvisioningPort,
                credentialProvisioningPort,
                identityCommandPort,
                adminBootstrapService,
                eventPublisher
        );
    }

    private TransferAdminAuthorityCommand validCommand() {
        return new TransferAdminAuthorityCommand(
                "replacement-admin@example.com",
                "Password01",
                "Replacement Administrator",
                "OptraBidz Operations",
                "Recovery transfer",
                null
        );
    }
}

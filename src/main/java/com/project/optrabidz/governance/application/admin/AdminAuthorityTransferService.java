package com.project.optrabidz.governance.application.admin;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.governance.application.admin.event.AdminAuthorityTransferredEvent;
import com.project.optrabidz.identity.application.command.DeactivateAccountCommand;
import com.project.optrabidz.identity.application.port.IdentityCommandPort;
import com.project.optrabidz.participation.application.port.AdminProvisioningPort;
import com.project.optrabidz.participation.domain.model.Admin;
import com.project.optrabidz.security.application.port.SecurityCredentialProvisioningPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Instant;

@Service
public class AdminAuthorityTransferService {
    private final AdministrativeAuthorityGuard authorityGuard;
    private final AdminProvisioningPort adminProvisioningPort;
    private final SecurityCredentialProvisioningPort credentialProvisioningPort;
    private final IdentityCommandPort identityCommandPort;
    private final AdminBootstrapService adminBootstrapService;
    private final EventPublisher eventPublisher;

    public AdminAuthorityTransferService(AdministrativeAuthorityGuard authorityGuard,
                                         AdminProvisioningPort adminProvisioningPort,
                                         SecurityCredentialProvisioningPort credentialProvisioningPort,
                                         IdentityCommandPort identityCommandPort,
                                         AdminBootstrapService adminBootstrapService,
                                         EventPublisher eventPublisher) {
        this.authorityGuard = authorityGuard;
        this.adminProvisioningPort = adminProvisioningPort;
        this.credentialProvisioningPort = credentialProvisioningPort;
        this.identityCommandPort = identityCommandPort;
        this.adminBootstrapService = adminBootstrapService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Long transferAuthority(TransferAdminAuthorityCommand command, boolean recoveryModeEnabled) {
        Assert.notNull(command, "TransferAdminAuthorityCommand must not be null");
        Assert.hasText(command.revocationReason(), "revocation reason must not be blank");
        authorityGuard.assertRecoveryTransferAllowed(recoveryModeEnabled);

        Admin revokedAdmin = adminProvisioningPort.revokeActiveAdmin(
                command.revokedByAccountId(),
                command.revocationReason()
        );
        credentialProvisioningPort.disableCredentialForAccount(revokedAdmin.getAccountId());
        identityCommandPort.deactivateAccount(new DeactivateAccountCommand(revokedAdmin.getAccountId()));

        Long newAdminAccountId = adminBootstrapService.createAdminAuthority(
                command.newAdminEmail(),
                command.newAdminRawPassword(),
                command.newPublicDisplayName(),
                command.newOrganizationLabel()
        );
        eventPublisher.publish(new AdminAuthorityTransferredEvent(
                newAdminAccountId,
                revokedAdmin.getAccountId(),
                command.revokedByAccountId(),
                command.revocationReason(),
                Instant.now()
        ));
        return newAdminAccountId;
    }
}

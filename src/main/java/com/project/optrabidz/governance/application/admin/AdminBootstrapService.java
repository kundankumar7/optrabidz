package com.project.optrabidz.governance.application.admin;

import com.project.optrabidz.identity.application.command.ActivateAccountCommand;
import com.project.optrabidz.identity.application.command.CreateAccountCommand;
import com.project.optrabidz.identity.application.command.UpdateProfileStatusCommand;
import com.project.optrabidz.identity.application.port.IdentityCommandPort;
import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.application.port.AdminProvisioningPort;
import com.project.optrabidz.security.application.command.ProvisionCredentialCommand;
import com.project.optrabidz.security.application.port.SecurityCredentialProvisioningPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;

@Service
public class AdminBootstrapService {
    private final AdministrativeAuthorityGuard authorityGuard;
    private final IdentityCommandPort identityCommandPort;
    private final SecurityCredentialProvisioningPort credentialProvisioningPort;
    private final AdminProvisioningPort adminProvisioningPort;

    public AdminBootstrapService(AdministrativeAuthorityGuard authorityGuard,
                                 IdentityCommandPort identityCommandPort,
                                 SecurityCredentialProvisioningPort credentialProvisioningPort,
                                 AdminProvisioningPort adminProvisioningPort) {
        this.authorityGuard = authorityGuard;
        this.identityCommandPort = identityCommandPort;
        this.credentialProvisioningPort = credentialProvisioningPort;
        this.adminProvisioningPort = adminProvisioningPort;
    }

    @Transactional
    public Optional<Long> bootstrapFirstAdmin(BootstrapAdminCommand command) {
        Assert.notNull(command, "BootstrapAdminCommand must not be null");

        if (!authorityGuard.canBootstrapFirstAdmin()) {
            return Optional.empty();
        }

        return Optional.of(createAdminAuthority(
                command.email(),
                command.rawPassword(),
                command.publicDisplayName(),
                command.organizationLabel()
        ));
    }

    Long createAdminAuthority(String email,
                              String rawPassword,
                              String publicDisplayName,
                              String organizationLabel) {
        Assert.hasText(email, "admin email must not be blank");
        Assert.hasText(rawPassword, "admin password must not be blank");
        Assert.hasText(publicDisplayName, "admin publicDisplayName must not be blank");

        Long accountId = identityCommandPort.createAccount(new CreateAccountCommand(RoleType.ADMIN));
        credentialProvisioningPort.createCredential(new ProvisionCredentialCommand(accountId, email, rawPassword));
        adminProvisioningPort.createActiveAdmin(accountId, publicDisplayName, organizationLabel);
        identityCommandPort.updateProfileStatus(new UpdateProfileStatusCommand(accountId, ProfileStatus.COMPLETE));
        identityCommandPort.activateAccount(new ActivateAccountCommand(accountId));
        return accountId;
    }
}

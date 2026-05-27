package com.project.optrabidz.security.application.port;

import com.project.optrabidz.security.application.command.ProvisionCredentialCommand;

public interface SecurityCredentialProvisioningPort {
    void createCredential(ProvisionCredentialCommand command);

    void disableCredentialForAccount(Long accountId);
}

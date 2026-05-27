package com.project.optrabidz.identity.application.port;

import com.project.optrabidz.identity.application.command.ActivateAccountCommand;
import com.project.optrabidz.identity.application.command.CreateAccountCommand;
import com.project.optrabidz.identity.application.command.DeactivateAccountCommand;
import com.project.optrabidz.identity.application.command.UpdateProfileStatusCommand;

public interface IdentityCommandPort {
    Long createAccount(CreateAccountCommand command);

    void activateAccount(ActivateAccountCommand command);

    void deactivateAccount(DeactivateAccountCommand command);

    void updateProfileStatus(UpdateProfileStatusCommand command);
}

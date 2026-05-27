package com.project.optrabidz.participation.application.port;

import com.project.optrabidz.participation.domain.model.Admin;

public interface AdminProvisioningPort {
    Admin createActiveAdmin(Long accountId, String publicDisplayName, String organizationLabel);

    Admin revokeActiveAdmin(Long revokedByAccountId, String reason);
}

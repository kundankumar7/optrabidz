package com.project.optrabidz.governance.application.admin;

public record TransferAdminAuthorityCommand(
        String newAdminEmail,
        String newAdminRawPassword,
        String newPublicDisplayName,
        String newOrganizationLabel,
        String revocationReason,
        Long revokedByAccountId
) {
}

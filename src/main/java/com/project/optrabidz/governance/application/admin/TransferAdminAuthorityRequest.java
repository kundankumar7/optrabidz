package com.project.optrabidz.governance.application.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TransferAdminAuthorityRequest(
        @NotBlank @Email String newAdminEmail,
        @NotBlank @Size(min = 8, max = 72) String newAdminRawPassword,
        @NotBlank String newPublicDisplayName,
        String newOrganizationLabel,
        @NotBlank String revocationReason,
        Long revokedByAccountId
) {
    public TransferAdminAuthorityCommand toCommand() {
        return new TransferAdminAuthorityCommand(
                newAdminEmail,
                newAdminRawPassword,
                newPublicDisplayName,
                newOrganizationLabel,
                revocationReason,
                revokedByAccountId
        );
    }
}

package com.project.optrabidz.governance.application.admin;

public record AdminTransferResponse(
        Long newAdminAccountId,
        String message
) {
}

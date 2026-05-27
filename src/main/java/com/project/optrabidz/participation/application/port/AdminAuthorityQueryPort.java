package com.project.optrabidz.participation.application.port;

import com.project.optrabidz.participation.domain.model.Admin;

import java.util.Optional;

public interface AdminAuthorityQueryPort {
    boolean activeAdminExists();

    boolean isActiveAdmin(Long accountId);

    Optional<Admin> findActiveAdmin();
}

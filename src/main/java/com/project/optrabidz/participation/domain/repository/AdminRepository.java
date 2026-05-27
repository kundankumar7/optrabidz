package com.project.optrabidz.participation.domain.repository;

import com.project.optrabidz.participation.domain.model.Admin;

import java.util.Optional;

public interface AdminRepository {
    Admin save(Admin admin);

    Optional<Admin> findByAccountId(Long accountId);

    Optional<Admin> findActiveAdmin();

    boolean existsByAccountId(Long accountId);

    boolean existsActiveAdmin();

    boolean existsActiveByAccountId(Long accountId);
}

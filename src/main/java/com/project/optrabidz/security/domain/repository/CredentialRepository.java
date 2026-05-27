package com.project.optrabidz.security.domain.repository;

import com.project.optrabidz.security.domain.model.Credential;

import java.util.Optional;

public interface CredentialRepository {
    Credential save(Credential credential);

    Optional<Credential> findByEmail(String email);

    Optional<Credential> findByAccountId(Long accountId);

    boolean existsByEmail(String email);
}

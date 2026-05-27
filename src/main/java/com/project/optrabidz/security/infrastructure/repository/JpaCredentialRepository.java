package com.project.optrabidz.security.infrastructure.repository;

import com.project.optrabidz.security.infrastructure.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaCredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByEmailIgnoreCase(String email);

    Optional<Credential> findByAccountId(Long accountId);

    boolean existsByEmailIgnoreCase(String email);
}

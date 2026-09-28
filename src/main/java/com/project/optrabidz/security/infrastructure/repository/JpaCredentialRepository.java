package com.project.optrabidz.security.infrastructure.repository;

import com.project.optrabidz.security.infrastructure.entity.Credential;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaCredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByEmailIgnoreCase(String email);

    Optional<Credential> findByAccountId(Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select credential from Credential credential where credential.accountId = :accountId")
    Optional<Credential> findByAccountIdForUpdate(@Param("accountId") Long accountId);

    boolean existsByEmailIgnoreCase(String email);
}

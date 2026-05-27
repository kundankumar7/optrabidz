package com.project.optrabidz.participation.infrastructure.repository;

import com.project.optrabidz.participation.infrastructure.entity.StartupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaStartupRepository extends JpaRepository<StartupEntity, Long> {
    boolean existsByAccountId(Long accountId);

    Optional<StartupEntity> findByAccountId(Long accountId);
}

package com.project.optrabidz.participation.infrastructure.repository;

import com.project.optrabidz.participation.domain.model.AdminState;
import com.project.optrabidz.participation.infrastructure.entity.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaAdminRepository extends JpaRepository<AdminEntity, Long> {
    Optional<AdminEntity> findByAccountId(Long accountId);

    Optional<AdminEntity> findFirstByAdminState(AdminState adminState);

    boolean existsByAccountId(Long accountId);

    boolean existsByAdminState(AdminState adminState);

    boolean existsByAccountIdAndAdminState(Long accountId, AdminState adminState);
}

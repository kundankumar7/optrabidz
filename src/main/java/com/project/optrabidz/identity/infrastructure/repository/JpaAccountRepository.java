package com.project.optrabidz.identity.infrastructure.repository;

import com.project.optrabidz.identity.infrastructure.entity.AccountEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaAccountRepository extends JpaRepository<AccountEntity, Long> {
    @EntityGraph(value = "Account.withRoleAndProfile", type = EntityGraph.EntityGraphType.LOAD)
    Optional<AccountEntity> findWithRoleAndProfileByAccountId(Long accountId);

    @EntityGraph(attributePaths = {"role"})
    Optional<AccountEntity> findWithRoleByAccountId(Long accountId);
}

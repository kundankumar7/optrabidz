package com.project.optrabidz.participation.infrastructure.repository;

import com.project.optrabidz.participation.infrastructure.entity.InvestorEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaInvestorRepository extends JpaRepository<InvestorEntity, Long> {
    boolean existsByAccountId(Long accountId);

    @EntityGraph(attributePaths = "webPresences")
    Optional<InvestorEntity> findByAccountId(Long accountId);
}

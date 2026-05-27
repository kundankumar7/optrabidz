package com.project.optrabidz.marketplace.infrastructure.repository;

import com.project.optrabidz.marketplace.infrastructure.entity.Agreement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaAgreementRepository extends JpaRepository<Agreement, Long> {
    Optional<Agreement> findByBidId(Long bidId);

    Page<Agreement> findByStartupId(Long startupId, Pageable pageable);

    Page<Agreement> findByInvestorId(Long investorId, Pageable pageable);
}

package com.project.optrabidz.marketplace.domain.repository;

import com.project.optrabidz.marketplace.domain.model.Agreement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AgreementRepository {
    Agreement save(Agreement agreement);

    Optional<Agreement> findById(Long agreementId);

    Optional<Agreement> findByBidId(Long bidId);

    Page<Agreement> findByStartupId(Long startupId, Pageable pageable);

    Page<Agreement> findByInvestorId(Long investorId, Pageable pageable);
}

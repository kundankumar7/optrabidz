package com.project.optrabidz.financial.domain.repository;

import com.project.optrabidz.financial.domain.model.Repayment;
import com.project.optrabidz.financial.domain.model.RepaymentProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface RepaymentRepository {
    Repayment save(Repayment repayment);

    Optional<Repayment> findById(Long repaymentId);

    Optional<Repayment> findByAgreementId(Long agreementId);

    Optional<RepaymentProgress> getProgressByAgreementId(Long agreementId);

    Page<Repayment> findByStartupId(Long startupId, Pageable pageable);

    Page<Repayment> findByInvestorId(Long investorId, Pageable pageable);

    int refreshStatus(Long repaymentId, Instant now);
}

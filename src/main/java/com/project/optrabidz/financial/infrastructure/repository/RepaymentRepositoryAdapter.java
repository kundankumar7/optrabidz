package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.Repayment;
import com.project.optrabidz.financial.domain.model.RepaymentProgress;
import com.project.optrabidz.financial.domain.model.RepaymentState;
import com.project.optrabidz.financial.domain.repository.RepaymentRepository;
import com.project.optrabidz.financial.infrastructure.mapper.FinancialPersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class RepaymentRepositoryAdapter implements RepaymentRepository {
    private final JpaRepaymentRepository jpaRepaymentRepository;
    private final FinancialPersistenceMapper mapper;

    public RepaymentRepositoryAdapter(JpaRepaymentRepository jpaRepaymentRepository,
                                      FinancialPersistenceMapper mapper) {
        this.jpaRepaymentRepository = jpaRepaymentRepository;
        this.mapper = mapper;
    }

    @Override
    public Repayment save(Repayment repayment) {
        return mapper.toDomain(jpaRepaymentRepository.save(mapper.toEntity(repayment)));
    }

    @Override
    public Optional<Repayment> findById(Long repaymentId) {
        return jpaRepaymentRepository.findById(repaymentId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Repayment> findByAgreementId(Long agreementId) {
        return jpaRepaymentRepository.findByAgreementId(agreementId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<RepaymentProgress> getProgressByAgreementId(Long agreementId) {
        return jpaRepaymentRepository.getProgressByAgreementId(agreementId)
                .map(this::toProgress);
    }

    @Override
    public Page<Repayment> findByStartupId(Long startupId, Pageable pageable) {
        return jpaRepaymentRepository.findByStartupId(startupId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Repayment> findByInvestorId(Long investorId, Pageable pageable) {
        return jpaRepaymentRepository.findByInvestorId(investorId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public int refreshStatus(Long repaymentId, Instant now) {
        return jpaRepaymentRepository.refreshStatus(repaymentId, now);
    }

    private RepaymentProgress toProgress(JpaRepaymentRepository.RepaymentProgressView view) {
        return new RepaymentProgress(
                view.getAgreementId(),
                view.getRepaymentId(),
                view.getStartupId(),
                view.getInvestorId(),
                view.getCurrencyCode(),
                view.getTotalInstallments().longValue(),
                view.getPaidInstallments().longValue(),
                view.getUnpaidInstallments().longValue(),
                view.getFailedInstallments().longValue(),
                view.getOverdueInstallments().longValue(),
                view.getCancelledInstallments().longValue(),
                view.getTotalAmount(),
                view.getPaidAmount(),
                view.getRemainingAmount(),
                RepaymentState.valueOf(view.getRepaymentStatus()),
                view.getNextInstallmentId(),
                view.getNextInstallmentNumber(),
                view.getNextDueAt()
        );
    }
}

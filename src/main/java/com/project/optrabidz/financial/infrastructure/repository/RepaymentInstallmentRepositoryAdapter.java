package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.RepaymentInstallment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
import com.project.optrabidz.financial.infrastructure.mapper.FinancialPersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class RepaymentInstallmentRepositoryAdapter implements RepaymentInstallmentRepository {
    private final JpaRepaymentInstallmentRepository jpaRepository;
    private final FinancialPersistenceMapper mapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RepaymentInstallmentRepositoryAdapter(JpaRepaymentInstallmentRepository jpaRepository,
                                                 FinancialPersistenceMapper mapper,
                                                 NamedParameterJdbcTemplate jdbcTemplate) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepaymentInstallment save(RepaymentInstallment installment) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(installment)));
    }

    @Override
    public List<RepaymentInstallment> saveAll(Collection<RepaymentInstallment> installments) {
        List<com.project.optrabidz.financial.infrastructure.entity.RepaymentInstallment> entities = installments.stream()
                .map(mapper::toEntity)
                .toList();
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RepaymentInstallment> findById(Long installmentId) {
        return jpaRepository.findById(installmentId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<RepaymentInstallment> findByIdForStartup(Long installmentId, Long startupId) {
        return jpaRepository.findByIdForStartup(installmentId, startupId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<RepaymentInstallment> findByIdForInvestor(Long installmentId, Long investorId) {
        return jpaRepository.findByIdForInvestor(installmentId, investorId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<RepaymentInstallment> findByRepaymentId(Long repaymentId, Pageable pageable) {
        return jpaRepository.findByRepaymentId(repaymentId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<RepaymentInstallment> findByRepaymentIdAndStates(Long repaymentId,
                                                                 Collection<RepaymentInstallmentState> states,
                                                                 Pageable pageable) {
        if (states == null || states.isEmpty()) {
            return findByRepaymentId(repaymentId, pageable);
        }
        return jpaRepository.findByRepaymentIdAndInstallmentStateIn(repaymentId, states, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<RepaymentInstallment> findByStartupId(Long startupId, Pageable pageable) {
        return jpaRepository.findByStartupId(startupId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<RepaymentInstallment> findByStartupIdAndStates(Long startupId,
                                                               Collection<RepaymentInstallmentState> states,
                                                               Pageable pageable) {
        if (states == null || states.isEmpty()) {
            return findByStartupId(startupId, pageable);
        }
        return jpaRepository.findByStartupIdAndInstallmentStateIn(startupId, states, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<RepaymentInstallment> findByInvestorId(Long investorId, Pageable pageable) {
        return jpaRepository.findByInvestorId(investorId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<RepaymentInstallment> findByInvestorIdAndStates(Long investorId,
                                                                Collection<RepaymentInstallmentState> states,
                                                                Pageable pageable) {
        if (states == null || states.isEmpty()) {
            return findByInvestorId(investorId, pageable);
        }
        return jpaRepository.findByInvestorIdAndInstallmentStateIn(investorId, states, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<RepaymentInstallment> findNextPayableByRepaymentId(Long repaymentId) {
        return jpaRepository.findNextPayableByRepaymentId(repaymentId)
                .map(mapper::toDomain);
    }

    @Override
    public int markPaymentInProgress(Long installmentId, Instant now) {
        return jpaRepository.markPaymentInProgress(installmentId, now);
    }

    @Override
    public int markPaid(Long installmentId, Long paymentIntentId, Instant now) {
        return jpaRepository.markPaid(installmentId, paymentIntentId, now);
    }

    @Override
    public int markPayoutPending(Long installmentId, Long paymentIntentId, Instant now) {
        return jpaRepository.markPayoutPending(installmentId, paymentIntentId, now);
    }

    @Override
    public int confirmPayoutPending(Long installmentId, Long paymentIntentId, Instant now) {
        return jpaRepository.confirmPayoutPending(installmentId, paymentIntentId, now);
    }

    @Override
    public int markPaymentFailed(Long installmentId, String reason, Instant now) {
        return jpaRepository.markPaymentFailed(installmentId, reason, now);
    }

    @Override
    public List<Long> findOverdueEligibleIds(Instant now, int batchSize) {
        if (batchSize <= 0) {
            return List.of();
        }
        return jpaRepository.findOverdueEligibleIds(now, batchSize);
    }

    @Override
    public List<Long> findRepaymentIdsByInstallmentIds(Collection<Long> installmentIds) {
        if (installmentIds == null || installmentIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findRepaymentIdsByInstallmentIds(installmentIds);
    }

    @Override
    @Transactional
    public List<Long> markOverdueReturning(Collection<Long> installmentIds, Instant now) {
        if (installmentIds == null || installmentIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.queryForList("""
                update repayment_installment
                set installment_status = 'OVERDUE',
                    overdue_at = coalesce(overdue_at, :now),
                    updated_at = :now
                where repayment_installment_id in (:installmentIds)
                  and due_at < :now
                  and installment_status in (
                    'NOT_STARTED'::repayment_installment_status_enum,
                    'PAYMENT_IN_PROGRESS'::repayment_installment_status_enum,
                    'PAYMENT_FAILED'::repayment_installment_status_enum
                  )
                returning repayment_installment_id
                """, new MapSqlParameterSource()
                .addValue("installmentIds", installmentIds)
                .addValue("now", Timestamp.from(now)), Long.class);
    }
}

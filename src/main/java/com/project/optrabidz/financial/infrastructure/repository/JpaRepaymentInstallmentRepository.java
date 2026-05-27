package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.infrastructure.entity.RepaymentInstallment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaRepaymentInstallmentRepository extends JpaRepository<RepaymentInstallment, Long> {
    Page<RepaymentInstallment> findByRepaymentId(Long repaymentId, Pageable pageable);

    Page<RepaymentInstallment> findByRepaymentIdAndInstallmentStateIn(Long repaymentId,
                                                                      Collection<RepaymentInstallmentState> states,
                                                                      Pageable pageable);

    @Query("""
            select ri
            from RepaymentInstallment ri
            join Repayment r on r.repaymentId = ri.repaymentId
            where r.startupId = :startupId
            """)
    Page<RepaymentInstallment> findByStartupId(@Param("startupId") Long startupId, Pageable pageable);

    @Query("""
            select ri
            from RepaymentInstallment ri
            join Repayment r on r.repaymentId = ri.repaymentId
            where r.startupId = :startupId
              and ri.installmentState in :states
            """)
    Page<RepaymentInstallment> findByStartupIdAndInstallmentStateIn(@Param("startupId") Long startupId,
                                                                    @Param("states") Collection<RepaymentInstallmentState> states,
                                                                    Pageable pageable);

    @Query("""
            select ri
            from RepaymentInstallment ri
            join Repayment r on r.repaymentId = ri.repaymentId
            where r.investorId = :investorId
            """)
    Page<RepaymentInstallment> findByInvestorId(@Param("investorId") Long investorId, Pageable pageable);

    @Query("""
            select ri
            from RepaymentInstallment ri
            join Repayment r on r.repaymentId = ri.repaymentId
            where r.investorId = :investorId
              and ri.installmentState in :states
            """)
    Page<RepaymentInstallment> findByInvestorIdAndInstallmentStateIn(@Param("investorId") Long investorId,
                                                                     @Param("states") Collection<RepaymentInstallmentState> states,
                                                                     Pageable pageable);

    @Query(value = """
            select *
            from repayment_installment
            where repayment_id = :repaymentId
              and installment_status in (
                'NOT_STARTED'::repayment_installment_status_enum,
                'PAYMENT_FAILED'::repayment_installment_status_enum,
                'OVERDUE'::repayment_installment_status_enum
              )
            order by due_at asc, installment_number asc
            limit 1
            """, nativeQuery = true)
    Optional<RepaymentInstallment> findNextPayableByRepaymentId(@Param("repaymentId") Long repaymentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update repayment_installment
            set installment_status = 'PAYMENT_IN_PROGRESS',
                payment_started_at = coalesce(payment_started_at, :now),
                failed_at = null,
                overdue_at = case when installment_status = 'OVERDUE'::repayment_installment_status_enum then overdue_at else null end,
                failure_reason = null,
                updated_at = :now
            where repayment_installment_id = :installmentId
              and installment_status in (
                'NOT_STARTED'::repayment_installment_status_enum,
                'PAYMENT_FAILED'::repayment_installment_status_enum,
                'OVERDUE'::repayment_installment_status_enum
              )
            """, nativeQuery = true)
    int markPaymentInProgress(@Param("installmentId") Long installmentId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update repayment_installment
            set installment_status = 'PAID',
                confirmed_payment_intent_id = :paymentIntentId,
                paid_at = :now,
                failed_at = null,
                failure_reason = null,
                updated_at = :now
            where repayment_installment_id = :installmentId
              and installment_status in (
                'NOT_STARTED'::repayment_installment_status_enum,
                'PAYMENT_IN_PROGRESS'::repayment_installment_status_enum,
                'PAYMENT_FAILED'::repayment_installment_status_enum,
                'OVERDUE'::repayment_installment_status_enum
              )
            """, nativeQuery = true)
    int markPaid(@Param("installmentId") Long installmentId,
                 @Param("paymentIntentId") Long paymentIntentId,
                 @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update repayment_installment
            set installment_status = case
                    when due_at <= :now then 'OVERDUE'::repayment_installment_status_enum
                    else 'PAYMENT_FAILED'::repayment_installment_status_enum
                end,
                failed_at = :now,
                overdue_at = case
                    when due_at <= :now then coalesce(overdue_at, :now)
                    else null
                end,
                failure_reason = :reason,
                updated_at = :now
            where repayment_installment_id = :installmentId
              and installment_status = 'PAYMENT_IN_PROGRESS'::repayment_installment_status_enum
            """, nativeQuery = true)
    int markPaymentFailed(@Param("installmentId") Long installmentId,
                          @Param("reason") String reason,
                          @Param("now") Instant now);

    @Query(value = """
            select repayment_installment_id
            from repayment_installment
            where due_at < :now
              and installment_status in (
                'NOT_STARTED'::repayment_installment_status_enum,
                'PAYMENT_IN_PROGRESS'::repayment_installment_status_enum,
                'PAYMENT_FAILED'::repayment_installment_status_enum
              )
            order by due_at asc, repayment_installment_id asc
            for update skip locked
            limit :batchSize
            """, nativeQuery = true)
    List<Long> findOverdueEligibleIds(@Param("now") Instant now, @Param("batchSize") int batchSize);

    @Query(value = """
            select distinct repayment_id
            from repayment_installment
            where repayment_installment_id in (:installmentIds)
            """, nativeQuery = true)
    List<Long> findRepaymentIdsByInstallmentIds(@Param("installmentIds") Collection<Long> installmentIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update repayment_installment
            set installment_status = 'OVERDUE',
                overdue_at = coalesce(overdue_at, :now),
                updated_at = :now
            where repayment_installment_id in (:installmentIds)
              and installment_status in (
                'NOT_STARTED'::repayment_installment_status_enum,
                'PAYMENT_IN_PROGRESS'::repayment_installment_status_enum,
                'PAYMENT_FAILED'::repayment_installment_status_enum
              )
            """, nativeQuery = true)
    int markOverdue(@Param("installmentIds") Collection<Long> installmentIds, @Param("now") Instant now);
}

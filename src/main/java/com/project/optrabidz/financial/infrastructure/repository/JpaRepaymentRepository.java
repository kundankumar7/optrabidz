package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.infrastructure.entity.Repayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface JpaRepaymentRepository extends JpaRepository<Repayment, Long> {
    Optional<Repayment> findByAgreementId(Long agreementId);

    Page<Repayment> findByStartupId(Long startupId, Pageable pageable);

    Page<Repayment> findByInvestorId(Long investorId, Pageable pageable);

    @Query(value = """
            select
                r.agreement_id as agreementId,
                r.repayment_id as repaymentId,
                r.startup_id as startupId,
                r.investor_id as investorId,
                r.currency_code as currencyCode,
                r.total_installments as totalInstallments,
                coalesce(sum(case when ri.installment_status = 'PAID'::repayment_installment_status_enum then 1 else 0 end), 0) as paidInstallments,
                coalesce(sum(case when ri.installment_status not in ('PAID'::repayment_installment_status_enum, 'CANCELLED'::repayment_installment_status_enum) then 1 else 0 end), 0) as unpaidInstallments,
                coalesce(sum(case when ri.installment_status = 'PAYMENT_FAILED'::repayment_installment_status_enum then 1 else 0 end), 0) as failedInstallments,
                coalesce(sum(case when ri.installment_status = 'OVERDUE'::repayment_installment_status_enum then 1 else 0 end), 0) as overdueInstallments,
                coalesce(sum(case when ri.installment_status = 'CANCELLED'::repayment_installment_status_enum then 1 else 0 end), 0) as cancelledInstallments,
                r.total_repayable_amount as totalAmount,
                coalesce(sum(case when ri.installment_status = 'PAID'::repayment_installment_status_enum then ri.amount else 0::numeric end), 0::numeric) as paidAmount,
                coalesce(sum(case when ri.installment_status not in ('PAID'::repayment_installment_status_enum, 'CANCELLED'::repayment_installment_status_enum) then ri.amount else 0::numeric end), 0::numeric) as remainingAmount,
                r.repayment_status::text as repaymentStatus,
                (
                    select next_i.repayment_installment_id
                    from repayment_installment next_i
                    where next_i.repayment_id = r.repayment_id
                      and next_i.installment_status in (
                        'NOT_STARTED'::repayment_installment_status_enum,
                        'PAYMENT_FAILED'::repayment_installment_status_enum,
                        'OVERDUE'::repayment_installment_status_enum
                      )
                    order by next_i.due_at asc, next_i.installment_number asc
                    limit 1
                ) as nextInstallmentId,
                (
                    select next_i.installment_number
                    from repayment_installment next_i
                    where next_i.repayment_id = r.repayment_id
                      and next_i.installment_status in (
                        'NOT_STARTED'::repayment_installment_status_enum,
                        'PAYMENT_FAILED'::repayment_installment_status_enum,
                        'OVERDUE'::repayment_installment_status_enum
                      )
                    order by next_i.due_at asc, next_i.installment_number asc
                    limit 1
                ) as nextInstallmentNumber,
                (
                    select next_i.due_at
                    from repayment_installment next_i
                    where next_i.repayment_id = r.repayment_id
                      and next_i.installment_status in (
                        'NOT_STARTED'::repayment_installment_status_enum,
                        'PAYMENT_FAILED'::repayment_installment_status_enum,
                        'OVERDUE'::repayment_installment_status_enum
                      )
                    order by next_i.due_at asc, next_i.installment_number asc
                    limit 1
                ) as nextDueAt
            from repayment r
            left join repayment_installment ri on ri.repayment_id = r.repayment_id
            where r.agreement_id = :agreementId
            group by r.repayment_id
            """, nativeQuery = true)
    Optional<RepaymentProgressView> getProgressByAgreementId(@Param("agreementId") Long agreementId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update repayment r
            set repayment_status = cast((
                    case
                        when exists (
                            select 1
                            from repayment_installment ri
                            where ri.repayment_id = r.repayment_id
                        ) and not exists (
                            select 1
                            from repayment_installment ri
                            where ri.repayment_id = r.repayment_id
                              and ri.installment_status <> 'PAID'::repayment_installment_status_enum
                        ) then 'COMPLETED'
                        when exists (
                            select 1
                            from repayment_installment ri
                            where ri.repayment_id = r.repayment_id
                              and ri.installment_status = 'OVERDUE'::repayment_installment_status_enum
                        ) then 'OVERDUE'
                        when exists (
                            select 1
                            from repayment_installment ri
                            where ri.repayment_id = r.repayment_id
                              and ri.installment_status = 'PAYMENT_FAILED'::repayment_installment_status_enum
                        ) then 'PAYMENT_ISSUE'
                        when exists (
                            select 1
                            from repayment_installment ri
                            where ri.repayment_id = r.repayment_id
                              and ri.installment_status in (
                                'PAYMENT_IN_PROGRESS'::repayment_installment_status_enum,
                                'PAID'::repayment_installment_status_enum
                              )
                        ) then 'IN_PROGRESS'
                        else 'NOT_STARTED'
                    end
                ) as repayment_status_enum),
                completed_at = case
                    when exists (
                        select 1
                        from repayment_installment ri
                        where ri.repayment_id = r.repayment_id
                    ) and not exists (
                        select 1
                        from repayment_installment ri
                        where ri.repayment_id = r.repayment_id
                          and ri.installment_status <> 'PAID'::repayment_installment_status_enum
                    ) then coalesce(r.completed_at, :now)
                    else null
                end,
                updated_at = :now
            where r.repayment_id = :repaymentId
            """, nativeQuery = true)
    int refreshStatus(@Param("repaymentId") Long repaymentId, @Param("now") Instant now);

    interface RepaymentProgressView {
        Long getAgreementId();

        Long getRepaymentId();

        Long getStartupId();

        Long getInvestorId();

        String getCurrencyCode();

        Number getTotalInstallments();

        Number getPaidInstallments();

        Number getUnpaidInstallments();

        Number getFailedInstallments();

        Number getOverdueInstallments();

        Number getCancelledInstallments();

        java.math.BigDecimal getTotalAmount();

        java.math.BigDecimal getPaidAmount();

        java.math.BigDecimal getRemainingAmount();

        String getRepaymentStatus();

        Long getNextInstallmentId();

        Integer getNextInstallmentNumber();

        Instant getNextDueAt();
    }
}

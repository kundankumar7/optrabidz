package com.project.optrabidz.financial.domain.repository;

import com.project.optrabidz.financial.domain.model.RepaymentInstallment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RepaymentInstallmentRepository {
    RepaymentInstallment save(RepaymentInstallment installment);

    List<RepaymentInstallment> saveAll(Collection<RepaymentInstallment> installments);

    Optional<RepaymentInstallment> findById(Long installmentId);

    Page<RepaymentInstallment> findByRepaymentId(Long repaymentId, Pageable pageable);

    Page<RepaymentInstallment> findByRepaymentIdAndStates(Long repaymentId,
                                                          Collection<RepaymentInstallmentState> states,
                                                          Pageable pageable);

    Page<RepaymentInstallment> findByStartupId(Long startupId, Pageable pageable);

    Page<RepaymentInstallment> findByStartupIdAndStates(Long startupId,
                                                        Collection<RepaymentInstallmentState> states,
                                                        Pageable pageable);

    Page<RepaymentInstallment> findByInvestorId(Long investorId, Pageable pageable);

    Page<RepaymentInstallment> findByInvestorIdAndStates(Long investorId,
                                                         Collection<RepaymentInstallmentState> states,
                                                         Pageable pageable);

    Optional<RepaymentInstallment> findNextPayableByRepaymentId(Long repaymentId);

    int markPaymentInProgress(Long installmentId, Instant now);

    int markPaid(Long installmentId, Long paymentIntentId, Instant now);

    int markPaymentFailed(Long installmentId, String reason, Instant now);

    List<Long> findOverdueEligibleIds(Instant now, int batchSize);

    List<Long> findRepaymentIdsByInstallmentIds(Collection<Long> installmentIds);

    int markOverdue(Collection<Long> installmentIds, Instant now);
}

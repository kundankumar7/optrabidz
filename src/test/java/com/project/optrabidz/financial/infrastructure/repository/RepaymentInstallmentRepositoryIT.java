package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.RepaymentInstallment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
import com.project.optrabidz.financial.infrastructure.mapper.FinancialPersistenceMapper;
import com.project.optrabidz.testsupport.PostgresJpaIntegrationTestSupport;
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import com.project.optrabidz.testsupport.PostgresTestDataFixture.Agreement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({FinancialPersistenceMapper.class, RepaymentInstallmentRepositoryAdapter.class})
class RepaymentInstallmentRepositoryIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-08-25T05:00:00Z");

    @Autowired
    private RepaymentInstallmentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PostgresTestDataFixture testData;

    @BeforeEach
    void setUpTestData() {
        testData = new PostgresTestDataFixture(jdbcTemplate, NOW);
    }

    @Test
    void findsInstallmentOnlyWithinOwningRepaymentParticipantScope() {
        Agreement owner = testData.createAgreement("installment owner");
        Agreement unrelated = testData.createAgreement("installment unrelated");
        Long installmentId = insertInstallment(insertRepayment(owner));

        assertThat(repository.findByIdForStartup(installmentId, owner.startupId()))
                .isPresent()
                .get()
                .extracting(RepaymentInstallment::getRepaymentInstallmentId)
                .isEqualTo(installmentId);
        assertThat(repository.findByIdForStartup(installmentId, unrelated.startupId())).isEmpty();
        assertThat(repository.findByIdForInvestor(installmentId, owner.investorId()))
                .isPresent()
                .get()
                .extracting(RepaymentInstallment::getRepaymentInstallmentId)
                .isEqualTo(installmentId);
        assertThat(repository.findByIdForInvestor(installmentId, unrelated.investorId())).isEmpty();
        assertThat(repository.findByIdForStartup(Long.MAX_VALUE, owner.startupId())).isEmpty();
        assertThat(repository.findByIdForInvestor(Long.MAX_VALUE, owner.investorId())).isEmpty();
        assertThat(repository.findById(installmentId)).isPresent();
    }

    @Test
    void markOverdueReturningReturnsOnlyChangedInstallments() {
        Agreement agreement = testData.createAgreement("overdue transition");
        Long repaymentId = insertRepayment(agreement);
        Long eligibleId = insertInstallment(repaymentId, 1, "NOT_STARTED", NOW.minusSeconds(60));
        Long alreadyOverdueId = insertInstallment(repaymentId, 2, "OVERDUE", NOW.minusSeconds(120));
        Long futureId = insertInstallment(repaymentId, 3, "NOT_STARTED", NOW.plusSeconds(60));

        List<Long> changedIds = repository.markOverdueReturning(
                List.of(eligibleId, alreadyOverdueId, futureId),
                NOW
        );

        assertThat(changedIds).containsExactly(eligibleId);
        assertThat(repository.findById(eligibleId))
                .isPresent()
                .get()
                .satisfies(installment -> {
                    assertThat(installment.getInstallmentState().name()).isEqualTo("OVERDUE");
                    assertThat(installment.getOverdueAt()).isEqualTo(NOW);
                });
        assertThat(repository.findById(futureId))
                .isPresent()
                .get()
                .satisfies(installment ->
                        assertThat(installment.getInstallmentState().name()).isEqualTo("NOT_STARTED"));
    }

    @Test
    void movesInstallmentFromPaymentInProgressThroughPayoutPendingToPaidConditionally() {
        Agreement agreement = testData.createAgreement("installment payout transition");
        Long installmentId = insertInstallment(insertRepayment(agreement));
        long paymentIntentId = 8001L;
        assertThat(repository.markPaymentInProgress(installmentId, NOW.plusSeconds(10))).isEqualTo(1);

        assertThat(repository.markPayoutPending(installmentId, paymentIntentId, NOW.plusSeconds(20)))
                .isEqualTo(1);
        assertThat(repository.markPayoutPending(installmentId, paymentIntentId, NOW.plusSeconds(21)))
                .isZero();
        assertThat(repository.findById(installmentId))
                .isPresent()
                .get()
                .satisfies(pending -> {
                    assertThat(pending.getInstallmentState()).isEqualTo(RepaymentInstallmentState.PAYOUT_PENDING);
                    assertThat(pending.getConfirmedPaymentIntentId()).isEqualTo(paymentIntentId);
                    assertThat(pending.getPaidAt()).isNull();
                });

        assertThat(repository.confirmPayoutPending(installmentId, paymentIntentId, NOW.plusSeconds(30)))
                .isEqualTo(1);
        assertThat(repository.confirmPayoutPending(installmentId, paymentIntentId, NOW.plusSeconds(31)))
                .isZero();
        assertThat(repository.findById(installmentId))
                .isPresent()
                .get()
                .satisfies(paid -> {
                    assertThat(paid.getInstallmentState()).isEqualTo(RepaymentInstallmentState.PAID);
                    assertThat(paid.getPaidAt()).isEqualTo(NOW.plusSeconds(30));
                });
    }

    @Test
    void paymentFailureBeforeDueKeepsInstallmentPayableForRetry() {
        Agreement agreement = testData.createAgreement("failure before due");
        Long installmentId = insertInstallment(
                insertRepayment(agreement), 1, "NOT_STARTED", NOW.plusSeconds(60));
        assertThat(repository.markPaymentInProgress(installmentId, NOW))
                .isEqualTo(1);

        assertThat(repository.markPaymentFailed(installmentId, "Payment cancelled", NOW))
                .isEqualTo(1);

        assertThat(repository.findById(installmentId))
                .isPresent()
                .get()
                .satisfies(installment -> {
                    assertThat(installment.getInstallmentState())
                            .isEqualTo(RepaymentInstallmentState.PAYMENT_FAILED);
                    assertThat(installment.getOverdueAt()).isNull();
                    assertThat(installment.getFailureReason()).isEqualTo("Payment cancelled");
                });
    }

    @Test
    void paymentFailureAtOrAfterDueMarksInstallmentOverdue() {
        Agreement agreement = testData.createAgreement("failure after due");
        Long installmentId = insertInstallment(
                insertRepayment(agreement), 1, "NOT_STARTED", NOW.minusSeconds(1));
        assertThat(repository.markPaymentInProgress(installmentId, NOW))
                .isEqualTo(1);

        assertThat(repository.markPaymentFailed(installmentId, "Payment cancelled", NOW))
                .isEqualTo(1);

        assertThat(repository.findById(installmentId))
                .isPresent()
                .get()
                .satisfies(installment -> {
                    assertThat(installment.getInstallmentState())
                            .isEqualTo(RepaymentInstallmentState.OVERDUE);
                    assertThat(installment.getOverdueAt()).isEqualTo(NOW);
                    assertThat(installment.getFailureReason()).isEqualTo("Payment cancelled");
                });
    }

    @Test
    void databaseRejectsFailureTimestampWhilePayoutIsPending() {
        Agreement agreement = testData.createAgreement("installment payout invariant");
        Long installmentId = insertInstallment(insertRepayment(agreement));
        assertThat(repository.markPaymentInProgress(installmentId, NOW.plusSeconds(10))).isEqualTo(1);
        assertThat(repository.markPayoutPending(installmentId, 8002L, NOW.plusSeconds(20))).isEqualTo(1);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                update repayment_installment
                set failed_at = ?
                where repayment_installment_id = ?
                """, Timestamp.from(NOW.plusSeconds(30)), installmentId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long insertRepayment(Agreement agreement) {
        return jdbcTemplate.queryForObject("""
                insert into repayment (
                    agreement_id, startup_id, investor_id, total_repayable_amount,
                    currency_code, total_installments, repayment_plan_type,
                    repayment_status, started_at, final_due_at, created_at, updated_at
                )
                values (?, ?, ?, 550000.00, 'INR', 1, 'INSTALLMENT_MONTHLY',
                        'NOT_STARTED', ?, ?, ?, ?)
                returning repayment_id
                """, Long.class,
                agreement.agreementId(), agreement.startupId(), agreement.investorId(),
                Timestamp.from(NOW), Timestamp.from(NOW.plusSeconds(86_400)),
                Timestamp.from(NOW), Timestamp.from(NOW));
    }

    private Long insertInstallment(Long repaymentId) {
        return insertInstallment(repaymentId, 1, "NOT_STARTED", NOW.plusSeconds(86_400));
    }

    private Long insertInstallment(Long repaymentId, int installmentNumber, String state, Instant dueAt) {
        Instant createdAt = dueAt.isBefore(NOW) ? dueAt.minusSeconds(3_600) : NOW;
        return jdbcTemplate.queryForObject("""
                insert into repayment_installment (
                    repayment_id, installment_number, installment_status, amount,
                    currency_code, due_at, overdue_at, created_at, updated_at
                )
                values (?, ?, ?::repayment_installment_status_enum, 550000.00, 'INR', ?,
                        case when ? = 'OVERDUE' then ?::timestamptz else null::timestamptz end, ?, ?)
                returning repayment_installment_id
                """, Long.class, repaymentId, installmentNumber, state, Timestamp.from(dueAt), state,
                Timestamp.from(NOW.minusSeconds(30)), Timestamp.from(createdAt),
                Timestamp.from(NOW));
    }
}

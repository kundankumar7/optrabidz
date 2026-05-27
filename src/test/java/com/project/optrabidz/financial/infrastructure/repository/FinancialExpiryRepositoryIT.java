package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.Repayment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallment;
import com.project.optrabidz.financial.domain.model.RepaymentInstallmentState;
import com.project.optrabidz.financial.domain.model.RepaymentState;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.model.Settlement;
import com.project.optrabidz.financial.domain.model.SettlementState;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
import com.project.optrabidz.financial.domain.repository.RepaymentRepository;
import com.project.optrabidz.financial.domain.repository.SettlementRepository;
import com.project.optrabidz.financial.infrastructure.mapper.FinancialPersistenceMapper;
import com.project.optrabidz.testsupport.PostgresJpaIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@Import({
        FinancialPersistenceMapper.class,
        PaymentIntentRepositoryAdapter.class,
        SettlementRepositoryAdapter.class,
        RepaymentRepositoryAdapter.class,
        RepaymentInstallmentRepositoryAdapter.class
})
class FinancialExpiryRepositoryIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-05-19T10:00:00Z");

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private RepaymentRepository repaymentRepository;

    @Autowired
    private RepaymentInstallmentRepository repaymentInstallmentRepository;

    @Autowired
    private PaymentIntentRepository paymentIntentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void settlementExpiryUpdatesOnlyExpiredPendingRowsInBatch() {
        Settlement expired = settlementRepository.save(settlement(
                101L,
                SettlementState.SETTLEMENT_PENDING,
                NOW.minusSeconds(900),
                null
        ));
        Settlement future = settlementRepository.save(settlement(
                102L,
                SettlementState.SETTLEMENT_PENDING,
                NOW.plusSeconds(900),
                null
        ));
        Settlement confirmed = settlementRepository.save(settlement(
                103L,
                SettlementState.SETTLEMENT_CONFIRMED,
                NOW.minusSeconds(900),
                NOW.minusSeconds(60)
        ));

        int expiredCount = settlementRepository.expireExpiredPending(NOW, 10);

        assertThat(expiredCount).isEqualTo(1);
        assertThat(settlementRepository.findById(expired.getSettlementId()))
                .isPresent()
                .get()
                .extracting(Settlement::getSettlementState)
                .isEqualTo(SettlementState.SETTLEMENT_EXPIRED);
        assertThat(settlementRepository.findById(future.getSettlementId()))
                .isPresent()
                .get()
                .extracting(Settlement::getSettlementState)
                .isEqualTo(SettlementState.SETTLEMENT_PENDING);
        assertThat(settlementRepository.findById(confirmed.getSettlementId()))
                .isPresent()
                .get()
                .extracting(Settlement::getSettlementState)
                .isEqualTo(SettlementState.SETTLEMENT_CONFIRMED);
    }

    @Test
    void settlementConfirmationRejectsExpiredPendingRows() {
        Settlement expired = settlementRepository.save(settlement(
                104L,
                SettlementState.SETTLEMENT_PENDING,
                NOW.minusSeconds(900),
                null
        ));

        int confirmedCount = settlementRepository.confirmPending(
                expired.getSettlementId(),
                9_001L,
                NOW
        );

        assertThat(confirmedCount).isZero();
        assertThat(settlementRepository.findById(expired.getSettlementId()))
                .isPresent()
                .get()
                .extracting(Settlement::getSettlementState)
                .isEqualTo(SettlementState.SETTLEMENT_PENDING);
    }

    @Test
    void repaymentInstallmentOverdueUpdatesOnlyEligibleRowsInBatch() {
        Repayment repayment = repaymentRepository.save(repayment(201L, RepaymentState.NOT_STARTED));
        RepaymentInstallment overdue = repaymentInstallmentRepository.save(installment(
                repayment.getRepaymentId(),
                1,
                RepaymentInstallmentState.NOT_STARTED,
                NOW.minusSeconds(900)
        ));
        RepaymentInstallment future = repaymentInstallmentRepository.save(installment(
                repayment.getRepaymentId(),
                2,
                RepaymentInstallmentState.NOT_STARTED,
                NOW.plusSeconds(900)
        ));
        RepaymentInstallment paid = repaymentInstallmentRepository.save(installment(
                repayment.getRepaymentId(),
                3,
                RepaymentInstallmentState.PAID,
                NOW.minusSeconds(900)
        ));

        List<Long> ids = repaymentInstallmentRepository.findOverdueEligibleIds(NOW, 10);
        int overdueCount = repaymentInstallmentRepository.markOverdue(ids, NOW);
        repaymentRepository.refreshStatus(repayment.getRepaymentId(), NOW);

        assertThat(overdueCount).isEqualTo(1);
        assertThat(repaymentInstallmentRepository.findById(overdue.getRepaymentInstallmentId()))
                .isPresent()
                .get()
                .extracting(RepaymentInstallment::getInstallmentState)
                .isEqualTo(RepaymentInstallmentState.OVERDUE);
        assertThat(repaymentInstallmentRepository.findById(future.getRepaymentInstallmentId()))
                .isPresent()
                .get()
                .extracting(RepaymentInstallment::getInstallmentState)
                .isEqualTo(RepaymentInstallmentState.NOT_STARTED);
        assertThat(repaymentInstallmentRepository.findById(paid.getRepaymentInstallmentId()))
                .isPresent()
                .get()
                .extracting(RepaymentInstallment::getInstallmentState)
                .isEqualTo(RepaymentInstallmentState.PAID);
        assertThat(repaymentRepository.findById(repayment.getRepaymentId()))
                .isPresent()
                .get()
                .extracting(Repayment::getRepaymentState)
                .isEqualTo(RepaymentState.OVERDUE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void paymentIntentExpiryCanRunInParallelWithoutDoubleProcessingRows() throws Exception {
        List<Long> expiredIntentIds = inTransaction(() -> List.of(
                paymentIntentRepository.save(paymentIntent(501L, PaymentState.CREATED, "parallel-intent-1")).getPaymentIntentId(),
                paymentIntentRepository.save(paymentIntent(502L, PaymentState.CREATED, "parallel-intent-2")).getPaymentIntentId(),
                paymentIntentRepository.save(paymentIntent(503L, PaymentState.PAYMENT_PENDING, "parallel-intent-3")).getPaymentIntentId(),
                paymentIntentRepository.save(paymentIntent(504L, PaymentState.PAYMENT_PENDING, "parallel-intent-4")).getPaymentIntentId(),
                paymentIntentRepository.save(paymentIntent(505L, PaymentState.CREATED, "parallel-intent-5")).getPaymentIntentId()
        ));

        int expiredCount = runTwoWorkers(() -> paymentIntentRepository.expireExpiredActive(NOW, 3));

        assertThat(expiredCount).isEqualTo(5);
        assertThat(inTransaction(() -> expiredIntentIds.stream()
                .map(paymentIntentRepository::findById)
                .map(value -> value.orElseThrow())
                .map(PaymentIntent::getPaymentState)
                .toList()))
                .containsOnly(PaymentState.PAYMENT_EXPIRED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void settlementExpiryCanRunInParallelWithoutDoubleProcessingRows() throws Exception {
        List<Long> expiredSettlementIds = inTransaction(() -> List.of(
                settlementRepository.save(settlement(601L, SettlementState.SETTLEMENT_PENDING, NOW.minusSeconds(900), null)).getSettlementId(),
                settlementRepository.save(settlement(602L, SettlementState.SETTLEMENT_PENDING, NOW.minusSeconds(800), null)).getSettlementId(),
                settlementRepository.save(settlement(603L, SettlementState.SETTLEMENT_PENDING, NOW.minusSeconds(700), null)).getSettlementId(),
                settlementRepository.save(settlement(604L, SettlementState.SETTLEMENT_PENDING, NOW.minusSeconds(600), null)).getSettlementId(),
                settlementRepository.save(settlement(605L, SettlementState.SETTLEMENT_PENDING, NOW.minusSeconds(500), null)).getSettlementId()
        ));

        int expiredCount = runTwoWorkers(() -> settlementRepository.expireExpiredPending(NOW, 3));

        assertThat(expiredCount).isEqualTo(5);
        assertThat(inTransaction(() -> expiredSettlementIds.stream()
                .map(settlementRepository::findById)
                .map(value -> value.orElseThrow())
                .map(Settlement::getSettlementState)
                .toList()))
                .containsOnly(SettlementState.SETTLEMENT_EXPIRED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void repaymentInstallmentOverdueCanRunInParallelWithoutDoubleProcessingRows() throws Exception {
        List<Long> installmentIds = inTransaction(() -> List.of(
                overdueInstallmentId(701L, 1),
                overdueInstallmentId(702L, 1),
                overdueInstallmentId(703L, 1),
                overdueInstallmentId(704L, 1),
                overdueInstallmentId(705L, 1)
        ));

        int overdueCount = runTwoWorkers(() -> {
            List<Long> ids = repaymentInstallmentRepository.findOverdueEligibleIds(NOW, 3);
            return repaymentInstallmentRepository.markOverdue(ids, NOW);
        });

        assertThat(overdueCount).isEqualTo(5);
        assertThat(inTransaction(() -> installmentIds.stream()
                .map(repaymentInstallmentRepository::findById)
                .map(value -> value.orElseThrow())
                .map(RepaymentInstallment::getInstallmentState)
                .toList()))
                .containsOnly(RepaymentInstallmentState.OVERDUE);
    }

    private int runTwoWorkers(Supplier<Integer> worker) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> task = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return inTransaction(worker);
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(task);
            Future<Integer> second = executor.submit(task);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return first.get() + second.get();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private <T> T inTransaction(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }

    private static PaymentIntent paymentIntent(Long settlementId, PaymentState state, String idempotencyKey) {
        return PaymentIntent.builder()
                .paymentPurpose(PaymentPurpose.SETTLEMENT)
                .settlementId(settlementId)
                .payerAccountId(2_000L + settlementId)
                .payeeAccountId(3_000L + settlementId)
                .amount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .paymentState(state)
                .idempotencyKey(idempotencyKey)
                .createdAt(NOW.minusSeconds(1_800))
                .expiresAt(NOW.minusSeconds(300))
                .build();
    }

    private static Settlement settlement(Long agreementId,
                                         SettlementState state,
                                         Instant expiresAt,
                                         Instant confirmedAt) {
        return Settlement.builder()
                .agreementId(agreementId)
                .startupId(301L + agreementId)
                .investorId(401L + agreementId)
                .amount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .settlementState(state)
                .createdAt(NOW.minusSeconds(1_800))
                .expiresAt(expiresAt)
                .confirmedAt(confirmedAt)
                .build();
    }

    private Long overdueInstallmentId(Long agreementId, Integer installmentNumber) {
        Repayment repayment = repaymentRepository.save(repayment(agreementId, RepaymentState.NOT_STARTED));
        return repaymentInstallmentRepository.save(installment(
                repayment.getRepaymentId(),
                installmentNumber,
                RepaymentInstallmentState.NOT_STARTED,
                NOW.minusSeconds(900)
        )).getRepaymentInstallmentId();
    }

    private static Repayment repayment(Long agreementId, RepaymentState state) {
        return Repayment.builder()
                .agreementId(agreementId)
                .startupId(501L + agreementId)
                .investorId(601L + agreementId)
                .totalRepayableAmount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .totalInstallments(3)
                .repaymentPlanType(com.project.optrabidz.marketplace.domain.model.RepaymentPlanType.INSTALLMENT_MONTHLY)
                .repaymentState(state)
                .startedAt(NOW.minusSeconds(1_800))
                .finalDueAt(NOW.plusSeconds(86_400))
                .createdAt(NOW.minusSeconds(1_800))
                .updatedAt(NOW.minusSeconds(1_800))
                .completedAt(state == RepaymentState.COMPLETED ? NOW.minusSeconds(60) : null)
                .build();
    }

    private static RepaymentInstallment installment(Long repaymentId,
                                                    Integer installmentNumber,
                                                    RepaymentInstallmentState state,
                                                    Instant dueAt) {
        return RepaymentInstallment.builder()
                .repaymentId(repaymentId)
                .installmentNumber(installmentNumber)
                .installmentState(state)
                .amount(new BigDecimal("100000.00"))
                .currencyCode("INR")
                .dueAt(dueAt)
                .paymentStartedAt(state == RepaymentInstallmentState.PAYMENT_IN_PROGRESS ? NOW.minusSeconds(100) : null)
                .paidAt(state == RepaymentInstallmentState.PAID ? NOW.minusSeconds(60) : null)
                .failedAt(state == RepaymentInstallmentState.PAYMENT_FAILED ? NOW.minusSeconds(60) : null)
                .overdueAt(state == RepaymentInstallmentState.OVERDUE ? NOW.minusSeconds(60) : null)
                .createdAt(NOW.minusSeconds(1_800))
                .updatedAt(NOW.minusSeconds(1_800))
                .build();
    }
}

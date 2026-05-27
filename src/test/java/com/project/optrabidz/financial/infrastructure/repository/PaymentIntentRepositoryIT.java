package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.infrastructure.mapper.FinancialPersistenceMapper;
import com.project.optrabidz.testsupport.PostgresJpaIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import({
        FinancialPersistenceMapper.class,
        PaymentIntentRepositoryAdapter.class
})
@Sql(scripts = "/db/test/finance-active-payment-intent-indexes.sql")
class PaymentIntentRepositoryIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-05-19T10:00:00Z");

    @Autowired
    private PaymentIntentRepository paymentIntentRepository;

    @Test
    void findActiveBySettlementIdReturnsCreatedOrPendingIntentOnly() {
        PaymentIntent created = paymentIntentRepository.save(settlementIntent(
                20_001L,
                PaymentState.CREATED,
                "active-created",
                NOW.minusSeconds(120),
                NOW.plusSeconds(900)
        ));
        PaymentIntent confirmed = settlementIntent(
                20_001L,
                PaymentState.PAYMENT_CONFIRMED,
                "confirmed",
                NOW,
                NOW.plusSeconds(900)
        );
        paymentIntentRepository.save(confirmed);

        Optional<PaymentIntent> found = paymentIntentRepository.findActiveBySettlementId(20_001L);

        assertThat(found).isPresent();
        assertThat(found.get().getPaymentIntentId()).isEqualTo(created.getPaymentIntentId());
        assertThat(found.get().getPaymentIntentId()).isNotEqualTo(confirmed.getPaymentIntentId());
        assertThat(found.get().getPaymentState()).isEqualTo(PaymentState.CREATED);
    }

    @Test
    void saveNewOrFindActiveBySettlementReturnsExistingActiveIntent() {
        PaymentIntent first = paymentIntentRepository.saveNewOrFindActiveBySettlement(settlementIntent(
                20_006L,
                PaymentState.CREATED,
                "settlement-intent-first",
                NOW.minusSeconds(120),
                NOW.plusSeconds(900)
        ));

        PaymentIntent second = paymentIntentRepository.saveNewOrFindActiveBySettlement(settlementIntent(
                20_006L,
                PaymentState.CREATED,
                "settlement-intent-second",
                NOW.minusSeconds(30),
                NOW.plusSeconds(900)
        ));

        assertThat(second.getPaymentIntentId()).isEqualTo(first.getPaymentIntentId());
        assertThat(second.getIdempotencyKey()).isEqualTo("settlement-intent-first");
    }

    @Test
    void saveNewOrFindActiveByRepaymentInstallmentReturnsExistingActiveIntent() {
        PaymentIntent first = paymentIntentRepository.saveNewOrFindActiveByRepaymentInstallment(repaymentIntent(
                30_007L,
                PaymentState.CREATED,
                "repayment-intent-first",
                NOW.minusSeconds(120),
                NOW.plusSeconds(900)
        ));

        PaymentIntent second = paymentIntentRepository.saveNewOrFindActiveByRepaymentInstallment(repaymentIntent(
                30_007L,
                PaymentState.CREATED,
                "repayment-intent-second",
                NOW.minusSeconds(30),
                NOW.plusSeconds(900)
        ));

        assertThat(second.getPaymentIntentId()).isEqualTo(first.getPaymentIntentId());
        assertThat(second.getIdempotencyKey()).isEqualTo("repayment-intent-first");
    }

    @Test
    void expireExpiredActiveUsesBatchLimitAndIgnoresFutureOrConfirmedIntents() {
        PaymentIntent expiredCreated = paymentIntentRepository.save(settlementIntent(
                20_002L,
                PaymentState.CREATED,
                "expired-created",
                NOW.minusSeconds(1_000),
                NOW.minusSeconds(300)
        ));
        PaymentIntent expiredPending = paymentIntentRepository.save(settlementIntent(
                20_003L,
                PaymentState.PAYMENT_PENDING,
                "expired-pending",
                NOW.minusSeconds(900),
                NOW.minusSeconds(100)
        ));
        paymentIntentRepository.save(settlementIntent(
                20_004L,
                PaymentState.CREATED,
                "future-created",
                NOW.minusSeconds(30),
                NOW.plusSeconds(900)
        ));
        PaymentIntent confirmed = paymentIntentRepository.save(settlementIntent(
                20_005L,
                PaymentState.PAYMENT_CONFIRMED,
                "expired-confirmed",
                NOW.minusSeconds(1_000),
                NOW.minusSeconds(300)
        ));

        int expiredCount = paymentIntentRepository.expireExpiredActive(NOW, 10);

        assertThat(expiredCount).isEqualTo(2);
        assertThat(paymentIntentRepository.findById(expiredCreated.getPaymentIntentId()))
                .isPresent()
                .get()
                .extracting(PaymentIntent::getPaymentState)
                .isEqualTo(PaymentState.PAYMENT_EXPIRED);
        assertThat(paymentIntentRepository.findById(expiredPending.getPaymentIntentId()))
                .isPresent()
                .get()
                .extracting(PaymentIntent::getPaymentState)
                .isEqualTo(PaymentState.PAYMENT_EXPIRED);
        assertThat(paymentIntentRepository.findById(confirmed.getPaymentIntentId()))
                .isPresent()
                .get()
                .extracting(PaymentIntent::getPaymentState)
                .isEqualTo(PaymentState.PAYMENT_CONFIRMED);
    }

    private static PaymentIntent settlementIntent(Long settlementId,
                                                  PaymentState state,
                                                  String idempotencyKey,
                                                  Instant createdAt,
                                                  Instant expiresAt) {
        return PaymentIntent.builder()
                .paymentPurpose(com.project.optrabidz.financial.domain.model.PaymentPurpose.SETTLEMENT)
                .settlementId(settlementId)
                .payerAccountId(220L + settlementId)
                .payeeAccountId(110L + settlementId)
                .amount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .paymentState(state)
                .idempotencyKey(idempotencyKey)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .confirmedAt(state == PaymentState.PAYMENT_CONFIRMED ? createdAt.plusSeconds(10) : null)
                .build();
    }

    private static PaymentIntent repaymentIntent(Long repaymentInstallmentId,
                                                 PaymentState state,
                                                 String idempotencyKey,
                                                 Instant createdAt,
                                                 Instant expiresAt) {
        return PaymentIntent.builder()
                .paymentPurpose(com.project.optrabidz.financial.domain.model.PaymentPurpose.REPAYMENT)
                .repaymentInstallmentId(repaymentInstallmentId)
                .payerAccountId(220L + repaymentInstallmentId)
                .payeeAccountId(110L + repaymentInstallmentId)
                .amount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .paymentState(state)
                .idempotencyKey(idempotencyKey)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .confirmedAt(state == PaymentState.PAYMENT_CONFIRMED ? createdAt.plusSeconds(10) : null)
                .build();
    }
}

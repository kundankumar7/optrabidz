package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.infrastructure.mapper.FinancialPersistenceMapper;
import com.project.optrabidz.testsupport.PostgresJpaIntegrationTestSupport;
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import com.project.optrabidz.testsupport.PostgresTestDataFixture.PaymentReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import({
        FinancialPersistenceMapper.class,
        PaymentIntentRepositoryAdapter.class
})
class PaymentIntentRepositoryIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-05-19T10:00:00Z");

    @Autowired
    private PaymentIntentRepository paymentIntentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PostgresTestDataFixture testData;

    @BeforeEach
    void setUpTestData() {
        testData = new PostgresTestDataFixture(jdbcTemplate, NOW);
    }

    @Test
    void findActiveBySettlementIdReturnsCreatedOrPendingIntentOnly() {
        PaymentReference settlement = testData.createSettlementReference("find-active");
        PaymentIntent created = paymentIntentRepository.save(settlementIntent(
                settlement,
                PaymentState.CREATED,
                "active-created",
                NOW.minusSeconds(120),
                NOW.plusSeconds(900)
        ));
        PaymentIntent confirmed = settlementIntent(
                settlement,
                PaymentState.PAYMENT_CONFIRMED,
                "confirmed",
                NOW,
                NOW.plusSeconds(900)
        );
        paymentIntentRepository.save(confirmed);

        Optional<PaymentIntent> found = paymentIntentRepository.findActiveBySettlementId(settlement.referenceId());

        assertThat(found).isPresent();
        assertThat(found.get().getPaymentIntentId()).isEqualTo(created.getPaymentIntentId());
        assertThat(found.get().getPaymentIntentId()).isNotEqualTo(confirmed.getPaymentIntentId());
        assertThat(found.get().getPaymentState()).isEqualTo(PaymentState.CREATED);
    }

    @Test
    void scopedLookupsExposeIntentOnlyToItsParticipantsAndPayer() {
        PaymentReference settlement = testData.createSettlementReference("payment-scope");
        PaymentReference unrelated = testData.createSettlementReference("payment-scope-unrelated");
        PaymentIntent saved = paymentIntentRepository.save(settlementIntent(
                settlement,
                PaymentState.CREATED,
                "payment-scope",
                NOW,
                NOW.plusSeconds(900)
        ));

        assertThat(paymentIntentRepository.findByIdForParticipant(
                saved.getPaymentIntentId(), settlement.payerAccountId())).isPresent();
        assertThat(paymentIntentRepository.findByIdForParticipant(
                saved.getPaymentIntentId(), settlement.payeeAccountId())).isPresent();
        assertThat(paymentIntentRepository.findByIdForParticipant(
                saved.getPaymentIntentId(), unrelated.payerAccountId())).isEmpty();
        assertThat(paymentIntentRepository.findByIdForPayer(
                saved.getPaymentIntentId(), settlement.payerAccountId())).isPresent();
        assertThat(paymentIntentRepository.findByIdForPayer(
                saved.getPaymentIntentId(), settlement.payeeAccountId())).isEmpty();
    }

    @Test
    void saveNewOrFindActiveBySettlementReturnsExistingActiveIntent() {
        PaymentReference settlement = testData.createSettlementReference("save-settlement");
        PaymentIntent first = paymentIntentRepository.saveNewOrFindActiveBySettlement(settlementIntent(
                settlement,
                PaymentState.CREATED,
                "settlement-intent-first",
                NOW.minusSeconds(120),
                NOW.plusSeconds(900)
        ));

        PaymentIntent second = paymentIntentRepository.saveNewOrFindActiveBySettlement(settlementIntent(
                settlement,
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
        PaymentReference installment = testData.createRepaymentInstallmentReference("save-repayment");
        PaymentIntent first = paymentIntentRepository.saveNewOrFindActiveByRepaymentInstallment(repaymentIntent(
                installment,
                PaymentState.CREATED,
                "repayment-intent-first",
                NOW.minusSeconds(120),
                NOW.plusSeconds(900)
        ));

        PaymentIntent second = paymentIntentRepository.saveNewOrFindActiveByRepaymentInstallment(repaymentIntent(
                installment,
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
        PaymentReference expiredCreatedSettlement = testData.createSettlementReference("expired-created");
        PaymentReference expiredPendingSettlement = testData.createSettlementReference("expired-pending");
        PaymentReference futureCreatedSettlement = testData.createSettlementReference("future-created");
        PaymentReference confirmedSettlement = testData.createSettlementReference("expired-confirmed");
        PaymentIntent expiredCreated = paymentIntentRepository.save(settlementIntent(
                expiredCreatedSettlement,
                PaymentState.CREATED,
                "expired-created",
                NOW.minusSeconds(1_000),
                NOW.minusSeconds(300)
        ));
        PaymentIntent expiredPending = paymentIntentRepository.save(settlementIntent(
                expiredPendingSettlement,
                PaymentState.PAYMENT_PENDING,
                "expired-pending",
                NOW.minusSeconds(900),
                NOW.minusSeconds(100)
        ));
        paymentIntentRepository.save(settlementIntent(
                futureCreatedSettlement,
                PaymentState.CREATED,
                "future-created",
                NOW.minusSeconds(30),
                NOW.plusSeconds(900)
        ));
        PaymentIntent confirmed = paymentIntentRepository.save(settlementIntent(
                confirmedSettlement,
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

    private static PaymentIntent settlementIntent(PaymentReference settlement,
                                                  PaymentState state,
                                                  String idempotencyKey,
                                                  Instant createdAt,
                                                  Instant expiresAt) {
        return PaymentIntent.builder()
                .paymentPurpose(com.project.optrabidz.financial.domain.model.PaymentPurpose.SETTLEMENT)
                .settlementId(settlement.referenceId())
                .payerAccountId(settlement.payerAccountId())
                .payeeAccountId(settlement.payeeAccountId())
                .amount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .paymentState(state)
                .idempotencyKey(idempotencyKey)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .confirmedAt(state == PaymentState.PAYMENT_CONFIRMED ? createdAt.plusSeconds(10) : null)
                .build();
    }

    private static PaymentIntent repaymentIntent(PaymentReference installment,
                                                 PaymentState state,
                                                 String idempotencyKey,
                                                 Instant createdAt,
                                                 Instant expiresAt) {
        return PaymentIntent.builder()
                .paymentPurpose(com.project.optrabidz.financial.domain.model.PaymentPurpose.REPAYMENT)
                .repaymentInstallmentId(installment.referenceId())
                .payerAccountId(installment.payerAccountId())
                .payeeAccountId(installment.payeeAccountId())
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

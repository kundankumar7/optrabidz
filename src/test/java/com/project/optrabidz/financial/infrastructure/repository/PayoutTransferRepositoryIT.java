package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;
import com.project.optrabidz.financial.domain.model.PayoutTransfer;
import com.project.optrabidz.financial.domain.model.PayoutTransferStatus;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.financial.domain.repository.PayoutTransferRepository;
import com.project.optrabidz.financial.infrastructure.mapper.PaymentAccountBindingPersistenceMapper;
import com.project.optrabidz.financial.infrastructure.mapper.PayoutTransferPersistenceMapper;
import com.project.optrabidz.testsupport.PostgresJpaIntegrationTestSupport;
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import com.project.optrabidz.testsupport.PostgresTestDataFixture.PaymentReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({
        PaymentAccountBindingPersistenceMapper.class,
        PaymentAccountBindingRepositoryAdapter.class,
        PayoutTransferPersistenceMapper.class,
        PayoutTransferRepositoryAdapter.class
})
class PayoutTransferRepositoryIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-10-01T09:00:00Z");

    @Autowired
    private PaymentAccountBindingRepository bindingRepository;

    @Autowired
    private PayoutTransferRepository payoutRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PaymentReference settlement;
    private PaymentAccountBinding binding;
    private Long paymentIntentId;

    @BeforeEach
    void setUp() {
        settlement = new PostgresTestDataFixture(jdbcTemplate, NOW)
                .createSettlementReference("payout repository");
        binding = PaymentAccountBinding.createPending(
                settlement.payeeAccountId(), "DEMO", "binding-command", null,
                "recipient-reference", "9876", "Demonstration Bank", NOW);
        binding.verify(NOW.plusSeconds(10));
        binding = bindingRepository.save(binding);
        paymentIntentId = insertConfirmedPaymentIntent();
    }

    @Test
    void claimsExpectedStateAndPersistsFailureThenConfirmationOnOneTransfer() {
        PayoutTransfer saved = payoutRepository.save(pendingTransfer("DEMO", "payout-key"));

        assertThat(payoutRepository.findByPaymentIntentId(paymentIntentId))
                .isPresent()
                .get()
                .extracting(PayoutTransfer::getPayoutTransferId)
                .isEqualTo(saved.getPayoutTransferId());

        assertThat(payoutRepository.claimForProcessing(
                saved.getPayoutTransferId(), PayoutTransferStatus.PENDING, NOW.plusSeconds(20)))
                .isEqualTo(1);
        assertThat(payoutRepository.claimForProcessing(
                saved.getPayoutTransferId(), PayoutTransferStatus.PENDING, NOW.plusSeconds(21)))
                .isZero();
        assertThat(payoutRepository.markFailed(
                saved.getPayoutTransferId(), "DEMO_DECLINED", "Demonstration failure", NOW.plusSeconds(30)))
                .isEqualTo(1);
        assertThat(payoutRepository.claimForProcessing(
                saved.getPayoutTransferId(), PayoutTransferStatus.FAILED, NOW.plusSeconds(40)))
                .isEqualTo(1);
        assertThat(payoutRepository.markConfirmed(
                saved.getPayoutTransferId(), "demo-transfer-reference", NOW.plusSeconds(50)))
                .isEqualTo(1);

        assertThat(payoutRepository.findById(saved.getPayoutTransferId()))
                .isPresent()
                .get()
                .satisfies(transfer -> {
                    assertThat(transfer.getTransferStatus()).isEqualTo(PayoutTransferStatus.CONFIRMED);
                    assertThat(transfer.getAttemptCount()).isEqualTo(2);
                    assertThat(transfer.getProviderTransferReference()).isEqualTo("demo-transfer-reference");
                    assertThat(transfer.getLatestFailureCode()).isEqualTo("DEMO_DECLINED");
                });
    }

    @Test
    void insertIfAbsentKeepsOneCanonicalTransferWithoutRaisingAConflict() {
        PayoutTransfer candidate = pendingTransfer("DEMO", "conflict-safe-payout-key");

        assertThat(payoutRepository.insertIfAbsent(candidate)).isTrue();
        assertThat(payoutRepository.insertIfAbsent(candidate)).isFalse();

        assertThat(payoutRepository.findByPaymentIntentId(paymentIntentId))
                .isPresent()
                .get()
                .satisfies(transfer -> {
                    assertThat(transfer.getPaymentAccountBindingId())
                            .isEqualTo(binding.getPaymentAccountBindingId());
                    assertThat(transfer.getProviderCode()).isEqualTo("DEMO");
                    assertThat(transfer.getIdempotencyKey())
                            .isEqualTo("conflict-safe-payout-key");
                });
    }

    @Test
    void capturedDestinationSurvivesLaterBindingDeactivation() {
        PayoutTransfer saved = payoutRepository.save(pendingTransfer(
                "DEMO", "captured-destination-key"));

        binding.deactivate(NOW.plusSeconds(20));
        bindingRepository.save(binding);

        assertThat(payoutRepository.findById(saved.getPayoutTransferId()))
                .isPresent()
                .get()
                .satisfies(transfer -> {
                    assertThat(transfer.getPaymentAccountBindingId())
                            .isEqualTo(binding.getPaymentAccountBindingId());
                    assertThat(transfer.getProviderCode()).isEqualTo("DEMO");
                });
    }

    @Test
    void databaseRejectsProviderDifferentFromCapturedBinding() {
        assertThatThrownBy(() -> payoutRepository.save(pendingTransfer("LOCAL", "mismatched-provider")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void claimRejectsAStateThatIsNotEligibleForProcessing() {
        PayoutTransfer saved = payoutRepository.save(pendingTransfer("DEMO", "invalid-claim-state"));
        assertThat(payoutRepository.claimForProcessing(
                saved.getPayoutTransferId(), PayoutTransferStatus.PENDING, NOW.plusSeconds(20)))
                .isEqualTo(1);

        assertThatThrownBy(() -> payoutRepository.claimForProcessing(
                saved.getPayoutTransferId(), PayoutTransferStatus.PROCESSING, NOW.plusSeconds(21)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedStatus");
    }

    @Test
    void databaseRejectsAClaimThatMovesUpdatedAtBackwards() {
        PayoutTransfer saved = payoutRepository.save(pendingTransfer("DEMO", "backward-claim"));
        assertThat(payoutRepository.claimForProcessing(
                saved.getPayoutTransferId(), PayoutTransferStatus.PENDING, NOW.plusSeconds(20)))
                .isEqualTo(1);
        assertThat(payoutRepository.markFailed(
                saved.getPayoutTransferId(), "DEMO_DECLINED", "Demonstration failure", NOW.plusSeconds(40)))
                .isEqualTo(1);

        assertThat(payoutRepository.claimForProcessing(
                saved.getPayoutTransferId(), PayoutTransferStatus.FAILED, NOW.plusSeconds(30)))
                .isZero();
    }

    @Test
    void databaseRejectsAConfirmationBeforeTheLastAttempt() {
        PayoutTransfer saved = payoutRepository.save(pendingTransfer("DEMO", "backward-confirmation"));
        assertThat(payoutRepository.claimForProcessing(
                saved.getPayoutTransferId(), PayoutTransferStatus.PENDING, NOW.plusSeconds(30)))
                .isEqualTo(1);

        assertThat(payoutRepository.markConfirmed(
                saved.getPayoutTransferId(), "demo-transfer-reference", NOW.plusSeconds(20)))
                .isZero();
    }

    @Test
    void databaseRejectsBlankProviderReference() {
        PayoutTransfer saved = payoutRepository.save(pendingTransfer("DEMO", "blank-provider-reference"));
        assertThat(payoutRepository.claimForProcessing(
                saved.getPayoutTransferId(), PayoutTransferStatus.PENDING, NOW.plusSeconds(20)))
                .isEqualTo(1);

        assertThatThrownBy(() -> payoutRepository.markConfirmed(
                saved.getPayoutTransferId(), "   ", NOW.plusSeconds(30)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsBlankFailureCode() {
        PayoutTransfer saved = payoutRepository.save(pendingTransfer("DEMO", "blank-failure-code"));
        assertThat(payoutRepository.claimForProcessing(
                saved.getPayoutTransferId(), PayoutTransferStatus.PENDING, NOW.plusSeconds(20)))
                .isEqualTo(1);

        assertThatThrownBy(() -> payoutRepository.markFailed(
                saved.getPayoutTransferId(), "   ", "Demonstration failure", NOW.plusSeconds(30)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private PayoutTransfer pendingTransfer(String providerCode, String idempotencyKey) {
        return PayoutTransfer.create(
                paymentIntentId,
                binding.getPaymentAccountBindingId(),
                providerCode,
                idempotencyKey,
                new BigDecimal("550000.00"),
                "INR",
                NOW.plusSeconds(15)
        );
    }

    private Long insertConfirmedPaymentIntent() {
        return jdbcTemplate.queryForObject("""
                insert into payment_intent(
                    payment_purpose, settlement_id, payer_account_id, payee_account_id,
                    amount, currency_code, payment_state, idempotency_key,
                    created_at, expires_at, confirmed_at
                )
                values ('SETTLEMENT', ?, ?, ?, 550000.00, 'INR', 'PAYMENT_CONFIRMED',
                        'confirmed-payout-intent', ?, ?, ?)
                returning payment_intent_id
                """, Long.class,
                settlement.referenceId(), settlement.payerAccountId(), settlement.payeeAccountId(),
                Timestamp.from(NOW), Timestamp.from(NOW.plusSeconds(900)), Timestamp.from(NOW.plusSeconds(5)));
    }
}

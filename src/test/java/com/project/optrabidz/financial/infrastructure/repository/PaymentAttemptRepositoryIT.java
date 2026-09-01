package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.repository.PaymentAttemptRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@Import({
        FinancialPersistenceMapper.class,
        PaymentIntentRepositoryAdapter.class,
        PaymentAttemptRepositoryAdapter.class
})
class PaymentAttemptRepositoryIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-05-19T10:00:00Z");

    @Autowired
    private PaymentIntentRepository paymentIntentRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PostgresTestDataFixture testData;

    @BeforeEach
    void setUpTestData() {
        testData = new PostgresTestDataFixture(jdbcTemplate, NOW);
    }

    @Test
    void scopedLookupsExposeAttemptOnlyToItsPayerAndProvider() {
        PaymentReference settlement = testData.createSettlementReference("attempt-scope");
        PaymentReference unrelated = testData.createSettlementReference("attempt-scope-unrelated");
        PaymentIntent intent = paymentIntentRepository.save(paymentIntent(settlement));
        PaymentAttempt attempt = paymentAttemptRepository.save(PaymentAttempt.create(
                intent.getPaymentIntentId(),
                "LOCAL",
                PaymentMethodType.BANK_TRANSFER,
                NOW
        ));

        assertThat(paymentAttemptRepository.findByIdForPayer(
                attempt.getPaymentAttemptId(), settlement.payerAccountId())).isPresent();
        assertThat(paymentAttemptRepository.findByIdForPayer(
                attempt.getPaymentAttemptId(), settlement.payeeAccountId())).isEmpty();
        assertThat(paymentAttemptRepository.findByIdForPayer(
                attempt.getPaymentAttemptId(), unrelated.payerAccountId())).isEmpty();
        assertThat(paymentAttemptRepository.findByIdForProvider(
                attempt.getPaymentAttemptId(), "local")).isPresent();
        assertThat(paymentAttemptRepository.findByIdForProvider(
                attempt.getPaymentAttemptId(), "external")).isEmpty();
    }

    private static PaymentIntent paymentIntent(PaymentReference settlement) {
        return PaymentIntent.builder()
                .paymentPurpose(PaymentPurpose.SETTLEMENT)
                .settlementId(settlement.referenceId())
                .payerAccountId(settlement.payerAccountId())
                .payeeAccountId(settlement.payeeAccountId())
                .amount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .paymentState(PaymentState.CREATED)
                .idempotencyKey("attempt-scope")
                .createdAt(NOW)
                .expiresAt(NOW.plusSeconds(900))
                .build();
    }
}

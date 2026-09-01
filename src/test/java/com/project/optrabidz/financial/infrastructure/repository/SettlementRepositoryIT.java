package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.Settlement;
import com.project.optrabidz.financial.domain.model.SettlementState;
import com.project.optrabidz.financial.domain.repository.SettlementRepository;
import com.project.optrabidz.financial.infrastructure.mapper.FinancialPersistenceMapper;
import com.project.optrabidz.testsupport.PostgresJpaIntegrationTestSupport;
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import com.project.optrabidz.testsupport.PostgresTestDataFixture.Agreement;
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
        SettlementRepositoryAdapter.class
})
class SettlementRepositoryIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-08-25T04:00:00Z");

    @Autowired
    private SettlementRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PostgresTestDataFixture testData;

    @BeforeEach
    void setUpTestData() {
        testData = new PostgresTestDataFixture(jdbcTemplate, NOW);
    }

    @Test
    void findsSettlementOnlyWithinRequestedStartupOrInvestorScope() {
        Agreement owner = testData.createAgreement("settlement owner");
        Agreement unrelated = testData.createAgreement("settlement unrelated");
        Settlement settlement = repository.save(pendingSettlement(owner));
        Long settlementId = settlement.getSettlementId();

        assertThat(repository.findByIdForStartup(
                settlementId, owner.startupId()))
                .isPresent()
                .get()
                .extracting(Settlement::getSettlementId)
                .isEqualTo(settlementId);
        assertThat(repository.findByIdForStartup(
                settlementId, unrelated.startupId())).isEmpty();
        assertThat(repository.findByIdForInvestor(
                settlementId, owner.investorId()))
                .isPresent()
                .get()
                .extracting(Settlement::getSettlementId)
                .isEqualTo(settlementId);
        assertThat(repository.findByIdForInvestor(
                settlementId, unrelated.investorId())).isEmpty();
        assertThat(repository.findByIdForStartup(
                Long.MAX_VALUE, owner.startupId())).isEmpty();
        assertThat(repository.findByIdForInvestor(
                Long.MAX_VALUE, owner.investorId())).isEmpty();
        assertThat(repository.findById(settlementId))
                .isPresent()
                .get()
                .extracting(Settlement::getSettlementId)
                .isEqualTo(settlementId);
    }

    private static Settlement pendingSettlement(Agreement agreement) {
        return Settlement.builder()
                .agreementId(agreement.agreementId())
                .startupId(agreement.startupId())
                .investorId(agreement.investorId())
                .amount(new BigDecimal("550000.00"))
                .currencyCode("INR")
                .settlementState(SettlementState.SETTLEMENT_PENDING)
                .createdAt(NOW)
                .expiresAt(NOW.plusSeconds(1_800))
                .build();
    }
}

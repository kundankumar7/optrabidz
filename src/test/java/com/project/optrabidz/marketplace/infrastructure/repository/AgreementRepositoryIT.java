package com.project.optrabidz.marketplace.infrastructure.repository;

import com.project.optrabidz.marketplace.domain.model.Agreement;
import com.project.optrabidz.marketplace.domain.repository.AgreementRepository;
import com.project.optrabidz.marketplace.infrastructure.mapper.MarketplacePersistenceMapper;
import com.project.optrabidz.testsupport.PostgresJpaIntegrationTestSupport;
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Import({MarketplacePersistenceMapper.class, AgreementRepositoryAdapter.class})
class AgreementRepositoryIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-08-25T05:00:00Z");

    @Autowired
    private AgreementRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PostgresTestDataFixture testData;

    @BeforeEach
    void setUpTestData() {
        testData = new PostgresTestDataFixture(jdbcTemplate, NOW);
    }

    @Test
    void findsAgreementOnlyWithinRequestedParticipantScope() {
        PostgresTestDataFixture.Agreement owner = agreementWithDebtTerms("agreement owner");
        PostgresTestDataFixture.Agreement unrelated = agreementWithDebtTerms("agreement unrelated");

        assertThat(repository.findByIdForStartup(owner.agreementId(), owner.startupId()))
                .isPresent()
                .get()
                .extracting(Agreement::getAgreementId)
                .isEqualTo(owner.agreementId());
        assertThat(repository.findByIdForStartup(owner.agreementId(), unrelated.startupId())).isEmpty();
        assertThat(repository.findByIdForInvestor(owner.agreementId(), owner.investorId()))
                .isPresent()
                .get()
                .extracting(Agreement::getAgreementId)
                .isEqualTo(owner.agreementId());
        assertThat(repository.findByIdForInvestor(owner.agreementId(), unrelated.investorId())).isEmpty();
        assertThat(repository.findByIdForStartup(Long.MAX_VALUE, owner.startupId())).isEmpty();
        assertThat(repository.findByIdForInvestor(Long.MAX_VALUE, owner.investorId())).isEmpty();
        assertThat(repository.findById(owner.agreementId())).isPresent();
    }

    private PostgresTestDataFixture.Agreement agreementWithDebtTerms(String label) {
        return testData.createAgreement(label);
    }
}

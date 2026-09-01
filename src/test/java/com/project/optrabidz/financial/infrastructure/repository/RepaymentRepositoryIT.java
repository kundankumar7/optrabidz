package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.Repayment;
import com.project.optrabidz.financial.domain.repository.RepaymentRepository;
import com.project.optrabidz.financial.infrastructure.mapper.FinancialPersistenceMapper;
import com.project.optrabidz.testsupport.PostgresJpaIntegrationTestSupport;
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import com.project.optrabidz.testsupport.PostgresTestDataFixture.Agreement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Import({FinancialPersistenceMapper.class, RepaymentRepositoryAdapter.class})
class RepaymentRepositoryIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-08-25T05:00:00Z");

    @Autowired
    private RepaymentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PostgresTestDataFixture testData;

    @BeforeEach
    void setUpTestData() {
        testData = new PostgresTestDataFixture(jdbcTemplate, NOW);
    }

    @Test
    void findsRepaymentOnlyWithinRequestedParticipantScope() {
        Agreement owner = testData.createAgreement("repayment owner");
        Agreement unrelated = testData.createAgreement("repayment unrelated");
        Long repaymentId = insertRepayment(owner);

        assertThat(repository.findByIdForStartup(repaymentId, owner.startupId()))
                .isPresent()
                .get()
                .extracting(Repayment::getRepaymentId)
                .isEqualTo(repaymentId);
        assertThat(repository.findByIdForStartup(repaymentId, unrelated.startupId())).isEmpty();
        assertThat(repository.findByIdForInvestor(repaymentId, owner.investorId()))
                .isPresent()
                .get()
                .extracting(Repayment::getRepaymentId)
                .isEqualTo(repaymentId);
        assertThat(repository.findByIdForInvestor(repaymentId, unrelated.investorId())).isEmpty();
        assertThat(repository.findByIdForStartup(Long.MAX_VALUE, owner.startupId())).isEmpty();
        assertThat(repository.findByIdForInvestor(Long.MAX_VALUE, owner.investorId())).isEmpty();
        assertThat(repository.findById(repaymentId)).isPresent();
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
}

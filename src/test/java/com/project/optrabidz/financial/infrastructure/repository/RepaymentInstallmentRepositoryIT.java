package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.RepaymentInstallment;
import com.project.optrabidz.financial.domain.repository.RepaymentInstallmentRepository;
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
        return jdbcTemplate.queryForObject("""
                insert into repayment_installment (
                    repayment_id, installment_number, installment_status, amount,
                    currency_code, due_at, created_at, updated_at
                )
                values (?, 1, 'NOT_STARTED', 550000.00, 'INR', ?, ?, ?)
                returning repayment_installment_id
                """, Long.class, repaymentId, Timestamp.from(NOW.plusSeconds(86_400)),
                Timestamp.from(NOW), Timestamp.from(NOW));
    }
}

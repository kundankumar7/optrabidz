package com.project.optrabidz.testsupport;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

public final class PostgresTestDataFixture {
    private final JdbcTemplate jdbcTemplate;
    private final Instant now;

    public PostgresTestDataFixture(JdbcTemplate jdbcTemplate, Instant now) {
        this.jdbcTemplate = jdbcTemplate;
        this.now = now;
    }

    public Startup createStartup(String label) {
        Long accountId = createAccount();
        Long startupId = jdbcTemplate.queryForObject("""
                insert into startup (
                    account_id, legal_entity_name, incorporation_country_code, public_display_name
                )
                values (?, ?, 'IN', ?)
                returning startup_id
                """, Long.class, accountId, label + " legal entity", label);
        return new Startup(accountId, startupId);
    }

    public Investor createInvestor(String label) {
        Long accountId = createAccount();
        Long investorId = jdbcTemplate.queryForObject("""
                insert into investor (account_id, public_display_name)
                values (?, ?)
                returning investor_id
                """, Long.class, accountId, label);
        return new Investor(accountId, investorId);
    }

    public Agreement createAgreement(String label) {
        Startup startup = createStartup(label + " startup");
        Investor investor = createInvestor(label + " investor");
        Long listingId = jdbcTemplate.queryForObject("""
                insert into funding_listing (
                    startup_id, listing_state, funding_model,
                    funding_purpose_description, title, created_at, published_at, closed_at
                )
                values (?, 'AGREEMENT_REACHED', 'DEBT', ?, ?, ?, ?, ?)
                returning listing_id
                """, Long.class, startup.startupId(), label + " purpose", label + " listing",
                timestamp(now.minusSeconds(3_600)), timestamp(now.minusSeconds(3_500)),
                timestamp(now.minusSeconds(2_500)));
        Long bidId = jdbcTemplate.queryForObject("""
                insert into bid (listing_id, investor_id, bid_state, created_at, accepted_at)
                values (?, ?, 'ACCEPTED', ?, ?)
                returning bid_id
                """, Long.class, listingId, investor.investorId(),
                timestamp(now.minusSeconds(3_000)), timestamp(now.minusSeconds(2_600)));
        Long agreementId = jdbcTemplate.queryForObject("""
                insert into agreement (listing_id, bid_id, startup_id, investor_id, created_at)
                values (?, ?, ?, ?, ?)
                returning agreement_id
                """, Long.class, listingId, bidId, startup.startupId(), investor.investorId(),
                timestamp(now.minusSeconds(2_400)));
        return new Agreement(
                startup.accountId(),
                investor.accountId(),
                startup.startupId(),
                investor.investorId(),
                agreementId
        );
    }

    public PaymentReference createSettlementReference(String label) {
        Agreement agreement = createAgreement(label);
        Long settlementId = jdbcTemplate.queryForObject("""
                insert into settlement (
                    agreement_id, settlement_state, startup_id, investor_id,
                    amount, currency_code, created_at, expires_at
                )
                values (?, 'SETTLEMENT_PENDING', ?, ?, 550000.00, 'INR', ?, ?)
                returning settlement_id
                """, Long.class,
                agreement.agreementId(), agreement.startupId(), agreement.investorId(),
                timestamp(now.minusSeconds(1_800)), timestamp(now.plusSeconds(1_800)));
        return new PaymentReference(
                settlementId,
                agreement.investorAccountId(),
                agreement.startupAccountId()
        );
    }

    public PaymentReference createRepaymentInstallmentReference(String label) {
        Agreement agreement = createAgreement(label);
        Long repaymentId = jdbcTemplate.queryForObject("""
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
                timestamp(now.minusSeconds(1_800)), timestamp(now.plusSeconds(86_400)),
                timestamp(now.minusSeconds(1_800)), timestamp(now.minusSeconds(1_800)));
        Long installmentId = jdbcTemplate.queryForObject("""
                insert into repayment_installment (
                    repayment_id, installment_number, installment_status, amount,
                    currency_code, due_at, created_at, updated_at
                )
                values (?, 1, 'NOT_STARTED', 550000.00, 'INR', ?, ?, ?)
                returning repayment_installment_id
                """, Long.class,
                repaymentId, timestamp(now.plusSeconds(86_400)),
                timestamp(now.minusSeconds(1_800)), timestamp(now.minusSeconds(1_800)));
        return new PaymentReference(
                installmentId,
                agreement.startupAccountId(),
                agreement.investorAccountId()
        );
    }

    private Long createAccount() {
        return jdbcTemplate.queryForObject("""
                insert into account (account_state, created_at)
                values ('ACTIVE', ?)
                returning account_id
                """, Long.class, timestamp(now.minusSeconds(3_600)));
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    public record Startup(Long accountId, Long startupId) {
    }

    public record Investor(Long accountId, Long investorId) {
    }

    public record Agreement(Long startupAccountId,
                            Long investorAccountId,
                            Long startupId,
                            Long investorId,
                            Long agreementId) {
    }

    public record PaymentReference(Long referenceId, Long payerAccountId, Long payeeAccountId) {
    }
}

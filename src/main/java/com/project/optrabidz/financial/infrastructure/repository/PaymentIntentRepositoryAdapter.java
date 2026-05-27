package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentState;
import com.project.optrabidz.financial.domain.repository.PaymentIntentRepository;
import com.project.optrabidz.financial.infrastructure.mapper.FinancialPersistenceMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentIntentRepositoryAdapter implements PaymentIntentRepository {
    private static final List<PaymentState> ACTIVE_STATES = List.of(
            PaymentState.CREATED,
            PaymentState.PAYMENT_PENDING
    );

    private final JpaPaymentIntentRepository jpaPaymentIntentRepository;
    private final FinancialPersistenceMapper mapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PaymentIntentRepositoryAdapter(JpaPaymentIntentRepository jpaPaymentIntentRepository,
                                          FinancialPersistenceMapper mapper,
                                          NamedParameterJdbcTemplate jdbcTemplate) {
        this.jpaPaymentIntentRepository = jpaPaymentIntentRepository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PaymentIntent save(PaymentIntent paymentIntent) {
        return mapper.toDomain(jpaPaymentIntentRepository.save(mapper.toEntity(paymentIntent)));
    }

    @Override
    public Optional<PaymentIntent> findById(Long paymentIntentId) {
        return jpaPaymentIntentRepository.findById(paymentIntentId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentIntent> findActiveBySettlementId(Long settlementId) {
        return jpaPaymentIntentRepository.findFirstBySettlementIdAndPaymentStateInOrderByCreatedAtDesc(
                        settlementId,
                        ACTIVE_STATES
                )
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentIntent> findActiveByRepaymentInstallmentId(Long repaymentInstallmentId) {
        return jpaPaymentIntentRepository.findFirstByRepaymentInstallmentIdAndPaymentStateInOrderByCreatedAtDesc(
                        repaymentInstallmentId,
                        ACTIVE_STATES
                )
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public PaymentIntent saveNewOrFindActiveBySettlement(PaymentIntent paymentIntent) {
        insertNewIfNoActiveConflict(paymentIntent);
        return findActiveBySettlementId(paymentIntent.getSettlementId())
                .orElseThrow(() -> new IllegalStateException("Active settlement payment intent could not be loaded"));
    }

    @Override
    @Transactional
    public PaymentIntent saveNewOrFindActiveByRepaymentInstallment(PaymentIntent paymentIntent) {
        insertNewIfNoActiveConflict(paymentIntent);
        return findActiveByRepaymentInstallmentId(paymentIntent.getRepaymentInstallmentId())
                .orElseThrow(() -> new IllegalStateException("Active repayment payment intent could not be loaded"));
    }

    @Override
    public List<Long> findExpiredActiveRepaymentInstallmentIds(Instant now, int batchSize) {
        if (batchSize <= 0) {
            return List.of();
        }
        return jpaPaymentIntentRepository.findExpiredActiveRepaymentInstallmentIds(now, batchSize);
    }

    @Override
    public int confirmActive(Long paymentIntentId, Instant now) {
        return jpaPaymentIntentRepository.confirmActive(paymentIntentId, now);
    }

    @Override
    public int failActive(Long paymentIntentId, String failureCode, String failureMessage, Instant now) {
        return jpaPaymentIntentRepository.failActive(paymentIntentId, failureCode, failureMessage, now);
    }

    @Override
    @Transactional
    public int expireExpiredActive(Instant now, int batchSize) {
        if (batchSize <= 0) {
            return 0;
        }
        List<Long> ids = jpaPaymentIntentRepository.findExpiredActiveIds(now, batchSize);
        if (ids.isEmpty()) {
            return 0;
        }
        return jpaPaymentIntentRepository.expireActive(ids, now);
    }

    private void insertNewIfNoActiveConflict(PaymentIntent paymentIntent) {
        jdbcTemplate.update("""
                insert into payment_intent (
                    payment_purpose,
                    settlement_id,
                    repayment_installment_id,
                    payer_account_id,
                    payee_account_id,
                    amount,
                    currency_code,
                    payment_state,
                    idempotency_key,
                    created_at,
                    expires_at
                )
                values (
                    cast(:paymentPurpose as payment_purpose_enum),
                    :settlementId,
                    :repaymentInstallmentId,
                    :payerAccountId,
                    :payeeAccountId,
                    :amount,
                    :currencyCode,
                    cast(:paymentState as payment_state_enum),
                    :idempotencyKey,
                    :createdAt,
                    :expiresAt
                )
                on conflict do nothing
                """, paymentIntentParameters(paymentIntent));
    }

    private MapSqlParameterSource paymentIntentParameters(PaymentIntent paymentIntent) {
        return new MapSqlParameterSource()
                .addValue("paymentPurpose", paymentIntent.getPaymentPurpose().name())
                .addValue("settlementId", paymentIntent.getSettlementId())
                .addValue("repaymentInstallmentId", paymentIntent.getRepaymentInstallmentId())
                .addValue("payerAccountId", paymentIntent.getPayerAccountId())
                .addValue("payeeAccountId", paymentIntent.getPayeeAccountId())
                .addValue("amount", paymentIntent.getAmount())
                .addValue("currencyCode", paymentIntent.getCurrencyCode())
                .addValue("paymentState", paymentIntent.getPaymentState().name())
                .addValue("idempotencyKey", paymentIntent.getIdempotencyKey())
                .addValue("createdAt", Timestamp.from(paymentIntent.getCreatedAt()))
                .addValue("expiresAt", Timestamp.from(paymentIntent.getExpiresAt()));
    }
}

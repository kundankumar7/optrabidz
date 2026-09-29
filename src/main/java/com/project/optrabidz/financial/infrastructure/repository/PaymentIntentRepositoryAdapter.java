package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.ExpiredPaymentIntentReference;
import com.project.optrabidz.financial.domain.model.PaymentIntent;
import com.project.optrabidz.financial.domain.model.PaymentPurpose;
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
    public Optional<PaymentIntent> findByIdForParticipant(Long paymentIntentId, Long accountId) {
        return jpaPaymentIntentRepository.findForParticipant(paymentIntentId, accountId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentIntent> findByIdForPayer(Long paymentIntentId, Long payerAccountId) {
        return jpaPaymentIntentRepository.findByPaymentIntentIdAndPayerAccountId(paymentIntentId, payerAccountId)
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
    public int confirmActive(Long paymentIntentId, Instant now) {
        return jpaPaymentIntentRepository.confirmActive(paymentIntentId, now);
    }

    @Override
    public int failActive(Long paymentIntentId, String failureCode, String failureMessage, Instant now) {
        return jpaPaymentIntentRepository.failActive(paymentIntentId, failureCode, failureMessage, now);
    }

    @Override
    @Transactional
    public List<ExpiredPaymentIntentReference> expireExpiredActiveReturning(
            Instant now,
            int batchSize
    ) {
        if (batchSize <= 0) {
            return List.of();
        }
        return jdbcTemplate.query("""
                with claimed as (
                    select payment_intent_id, expires_at
                    from payment_intent
                    where payment_state in (
                        'CREATED'::payment_state_enum,
                        'PAYMENT_PENDING'::payment_state_enum
                    )
                      and expires_at <= :now
                    order by expires_at asc, payment_intent_id asc
                    for update skip locked
                    limit :batchSize
                ), expired as (
                    update payment_intent intent
                    set payment_state = 'PAYMENT_EXPIRED',
                        expired_at = :now
                    from claimed
                    where intent.payment_intent_id = claimed.payment_intent_id
                      and intent.payment_state in (
                        'CREATED'::payment_state_enum,
                        'PAYMENT_PENDING'::payment_state_enum
                      )
                      and intent.expires_at <= :now
                    returning intent.payment_intent_id,
                              intent.payment_purpose::text as payment_purpose,
                              intent.settlement_id,
                              intent.repayment_installment_id
                )
                select expired.payment_intent_id,
                       expired.payment_purpose,
                       expired.settlement_id,
                       expired.repayment_installment_id
                from expired
                join claimed using (payment_intent_id)
                order by claimed.expires_at asc, expired.payment_intent_id asc
                """, new MapSqlParameterSource()
                .addValue("now", Timestamp.from(now))
                .addValue("batchSize", batchSize), this::mapExpiredReference);
    }

    @Override
    @Transactional
    public Optional<ExpiredPaymentIntentReference> expireActiveByIdReturning(
            Long paymentIntentId,
            Instant now
    ) {
        return jdbcTemplate.query("""
                update payment_intent
                set payment_state = 'PAYMENT_EXPIRED',
                    expired_at = :now
                where payment_intent_id = :paymentIntentId
                  and payment_state in (
                    'CREATED'::payment_state_enum,
                    'PAYMENT_PENDING'::payment_state_enum
                  )
                  and expires_at <= :now
                returning payment_intent_id,
                          payment_purpose::text as payment_purpose,
                          settlement_id,
                          repayment_installment_id
                """, new MapSqlParameterSource()
                .addValue("paymentIntentId", paymentIntentId)
                .addValue("now", Timestamp.from(now)), this::mapExpiredReference)
                .stream()
                .findFirst();
    }

    private ExpiredPaymentIntentReference mapExpiredReference(
            java.sql.ResultSet resultSet,
            int rowNumber
    ) throws java.sql.SQLException {
        return new ExpiredPaymentIntentReference(
                resultSet.getLong("payment_intent_id"),
                PaymentPurpose.valueOf(resultSet.getString("payment_purpose")),
                resultSet.getObject("settlement_id", Long.class),
                resultSet.getObject("repayment_installment_id", Long.class)
        );
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

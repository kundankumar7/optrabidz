package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.PayoutTransfer;
import com.project.optrabidz.financial.domain.model.PayoutTransferStatus;
import com.project.optrabidz.financial.domain.repository.PayoutTransferRepository;
import com.project.optrabidz.financial.infrastructure.mapper.PayoutTransferPersistenceMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class PayoutTransferRepositoryAdapter implements PayoutTransferRepository {
    private final JpaPayoutTransferRepository jpaRepository;
    private final PayoutTransferPersistenceMapper mapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PayoutTransferRepositoryAdapter(JpaPayoutTransferRepository jpaRepository,
                                           PayoutTransferPersistenceMapper mapper,
                                           NamedParameterJdbcTemplate jdbcTemplate) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PayoutTransfer save(PayoutTransfer payoutTransfer) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(payoutTransfer)));
    }

    @Override
    public boolean insertIfAbsent(PayoutTransfer payoutTransfer) {
        int inserted = jdbcTemplate.update("""
                insert into payout_transfer (
                    payment_intent_id,
                    payment_account_binding_id,
                    provider_code,
                    idempotency_key,
                    amount,
                    currency_code,
                    transfer_status,
                    attempt_count,
                    created_at,
                    updated_at
                ) values (
                    :paymentIntentId,
                    :paymentAccountBindingId,
                    :providerCode,
                    :idempotencyKey,
                    :amount,
                    :currencyCode,
                    cast(:transferStatus as payout_transfer_status_enum),
                    :attemptCount,
                    :createdAt,
                    :updatedAt
                )
                on conflict do nothing
                """, new MapSqlParameterSource()
                .addValue("paymentIntentId", payoutTransfer.getPaymentIntentId())
                .addValue("paymentAccountBindingId", payoutTransfer.getPaymentAccountBindingId())
                .addValue("providerCode", payoutTransfer.getProviderCode())
                .addValue("idempotencyKey", payoutTransfer.getIdempotencyKey())
                .addValue("amount", payoutTransfer.getAmount())
                .addValue("currencyCode", payoutTransfer.getCurrencyCode())
                .addValue("transferStatus", payoutTransfer.getTransferStatus().name())
                .addValue("attemptCount", payoutTransfer.getAttemptCount())
                .addValue("createdAt", Timestamp.from(payoutTransfer.getCreatedAt()))
                .addValue("updatedAt", Timestamp.from(payoutTransfer.getUpdatedAt())));
        return inserted == 1;
    }

    @Override
    public Optional<PayoutTransfer> findById(Long payoutTransferId) {
        return jpaRepository.findById(payoutTransferId).map(mapper::toDomain);
    }

    @Override
    public Optional<PayoutTransfer> findByPaymentIntentId(Long paymentIntentId) {
        return jpaRepository.findByPaymentIntentId(paymentIntentId).map(mapper::toDomain);
    }

    @Override
    public int claimForProcessing(Long payoutTransferId, PayoutTransferStatus expectedStatus, Instant now) {
        if (expectedStatus != PayoutTransferStatus.PENDING && expectedStatus != PayoutTransferStatus.FAILED) {
            throw new IllegalArgumentException("expectedStatus must be PENDING or FAILED");
        }
        return jpaRepository.claimForProcessing(payoutTransferId, expectedStatus.name(), now);
    }

    @Override
    public int markConfirmed(Long payoutTransferId, String providerReference, Instant now) {
        return jpaRepository.markConfirmed(payoutTransferId, providerReference, now);
    }

    @Override
    public int markFailed(Long payoutTransferId, String failureCode, String failureMessage, Instant now) {
        return jpaRepository.markFailed(payoutTransferId, failureCode, failureMessage, now);
    }
}

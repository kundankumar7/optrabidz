package com.project.optrabidz.financial.infrastructure.mapper;

import com.project.optrabidz.financial.domain.model.PayoutTransfer;
import org.springframework.stereotype.Component;

@Component
public class PayoutTransferPersistenceMapper {
    public com.project.optrabidz.financial.infrastructure.entity.PayoutTransfer toEntity(PayoutTransfer transfer) {
        var entity = new com.project.optrabidz.financial.infrastructure.entity.PayoutTransfer();
        entity.setPayoutTransferId(transfer.getPayoutTransferId());
        entity.setPaymentIntentId(transfer.getPaymentIntentId());
        entity.setPaymentAccountBindingId(transfer.getPaymentAccountBindingId());
        entity.setProviderCode(transfer.getProviderCode());
        entity.setProviderTransferReference(transfer.getProviderTransferReference());
        entity.setIdempotencyKey(transfer.getIdempotencyKey());
        entity.setAmount(transfer.getAmount());
        entity.setCurrencyCode(transfer.getCurrencyCode());
        entity.setTransferStatus(transfer.getTransferStatus());
        entity.setAttemptCount(transfer.getAttemptCount());
        entity.setCreatedAt(transfer.getCreatedAt());
        entity.setLastAttemptAt(transfer.getLastAttemptAt());
        entity.setConfirmedAt(transfer.getConfirmedAt());
        entity.setLatestFailureAt(transfer.getLatestFailureAt());
        entity.setLatestFailureCode(transfer.getLatestFailureCode());
        entity.setLatestFailureMessage(transfer.getLatestFailureMessage());
        entity.setUpdatedAt(transfer.getUpdatedAt());
        return entity;
    }

    public PayoutTransfer toDomain(com.project.optrabidz.financial.infrastructure.entity.PayoutTransfer entity) {
        return PayoutTransfer.builder()
                .payoutTransferId(entity.getPayoutTransferId())
                .paymentIntentId(entity.getPaymentIntentId())
                .paymentAccountBindingId(entity.getPaymentAccountBindingId())
                .providerCode(entity.getProviderCode())
                .providerTransferReference(entity.getProviderTransferReference())
                .idempotencyKey(entity.getIdempotencyKey())
                .amount(entity.getAmount())
                .currencyCode(entity.getCurrencyCode())
                .transferStatus(entity.getTransferStatus())
                .attemptCount(entity.getAttemptCount())
                .createdAt(entity.getCreatedAt())
                .lastAttemptAt(entity.getLastAttemptAt())
                .confirmedAt(entity.getConfirmedAt())
                .latestFailureAt(entity.getLatestFailureAt())
                .latestFailureCode(entity.getLatestFailureCode())
                .latestFailureMessage(entity.getLatestFailureMessage())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

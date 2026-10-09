package com.project.optrabidz.financial.infrastructure.mapper;

import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;
import org.springframework.stereotype.Component;

@Component
public class PaymentAccountBindingPersistenceMapper {
    public com.project.optrabidz.financial.infrastructure.entity.PaymentAccountBinding toEntity(
            PaymentAccountBinding binding
    ) {
        var entity = new com.project.optrabidz.financial.infrastructure.entity.PaymentAccountBinding();
        entity.setPaymentAccountBindingId(binding.getPaymentAccountBindingId());
        entity.setAccountId(binding.getAccountId());
        entity.setProviderCode(binding.getProviderCode());
        entity.setCommandIdempotencyKey(binding.getCommandIdempotencyKey());
        entity.setReplacesBindingId(binding.getReplacesBindingId());
        entity.setExternalRecipientReference(binding.getExternalRecipientReference());
        entity.setBindingStatus(binding.getBindingStatus());
        entity.setMaskedAccountSuffix(binding.getMaskedAccountSuffix());
        entity.setDemoBankLabel(binding.getDemoBankLabel());
        entity.setCreatedAt(binding.getCreatedAt());
        entity.setVerifiedAt(binding.getVerifiedAt());
        entity.setDeactivatedAt(binding.getDeactivatedAt());
        entity.setLockVersion(binding.getLockVersion());
        return entity;
    }

    public PaymentAccountBinding toDomain(
            com.project.optrabidz.financial.infrastructure.entity.PaymentAccountBinding entity
    ) {
        return PaymentAccountBinding.builder()
                .paymentAccountBindingId(entity.getPaymentAccountBindingId())
                .accountId(entity.getAccountId())
                .providerCode(entity.getProviderCode())
                .commandIdempotencyKey(entity.getCommandIdempotencyKey())
                .replacesBindingId(entity.getReplacesBindingId())
                .externalRecipientReference(entity.getExternalRecipientReference())
                .bindingStatus(entity.getBindingStatus())
                .maskedAccountSuffix(entity.getMaskedAccountSuffix())
                .demoBankLabel(entity.getDemoBankLabel())
                .createdAt(entity.getCreatedAt())
                .verifiedAt(entity.getVerifiedAt())
                .deactivatedAt(entity.getDeactivatedAt())
                .lockVersion(entity.getLockVersion())
                .build();
    }
}

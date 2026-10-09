package com.project.optrabidz.financial.domain.repository;

import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;

import java.util.Optional;

public interface PaymentAccountBindingRepository {
    boolean lockAccountForBindingChange(Long accountId);

    PaymentAccountBinding save(PaymentAccountBinding binding);

    Optional<PaymentAccountBinding> findById(Long bindingId);

    Optional<PaymentAccountBinding> findByAccountIdAndProviderCodeAndActive(Long accountId, String providerCode);

    Optional<PaymentAccountBinding> findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
            Long accountId,
            String providerCode,
            String commandIdempotencyKey
    );

    Optional<PaymentAccountBinding> findVerifiedByAccountIdAndProviderCode(Long accountId, String providerCode);
}

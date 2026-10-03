package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;
import com.project.optrabidz.financial.domain.model.PaymentAccountBindingStatus;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.financial.infrastructure.mapper.PaymentAccountBindingPersistenceMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PaymentAccountBindingRepositoryAdapter implements PaymentAccountBindingRepository {
    private final JpaPaymentAccountBindingRepository jpaRepository;
    private final PaymentAccountBindingPersistenceMapper mapper;

    public PaymentAccountBindingRepositoryAdapter(JpaPaymentAccountBindingRepository jpaRepository,
                                                  PaymentAccountBindingPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public boolean lockAccountForBindingChange(Long accountId) {
        return jpaRepository.lockAccountForBindingChange(accountId).isPresent();
    }

    @Override
    public PaymentAccountBinding save(PaymentAccountBinding binding) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(binding)));
    }

    @Override
    public Optional<PaymentAccountBinding> findById(Long bindingId) {
        return jpaRepository.findById(bindingId).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentAccountBinding> findByAccountIdAndProviderCodeAndActive(
            Long accountId,
            String providerCode
    ) {
        return jpaRepository.findFirstByAccountIdAndProviderCodeAndBindingStatusNot(
                        accountId, providerCode, PaymentAccountBindingStatus.DEACTIVATED)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentAccountBinding> findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
            Long accountId,
            String providerCode,
            String commandIdempotencyKey
    ) {
        return jpaRepository.findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
                        accountId, providerCode, commandIdempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentAccountBinding> findVerifiedByAccountIdAndProviderCode(
            Long accountId,
            String providerCode
    ) {
        return jpaRepository.findFirstByAccountIdAndProviderCodeAndBindingStatus(
                        accountId, providerCode, PaymentAccountBindingStatus.VERIFIED)
                .map(mapper::toDomain);
    }
}

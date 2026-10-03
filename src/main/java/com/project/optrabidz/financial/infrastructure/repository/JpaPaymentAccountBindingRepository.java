package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.PaymentAccountBindingStatus;
import com.project.optrabidz.financial.infrastructure.entity.PaymentAccountBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaPaymentAccountBindingRepository extends JpaRepository<PaymentAccountBinding, Long> {
    @Query(value = "select account_id from account where account_id = :accountId for update", nativeQuery = true)
    Optional<Long> lockAccountForBindingChange(@Param("accountId") Long accountId);

    Optional<PaymentAccountBinding> findFirstByAccountIdAndProviderCodeAndBindingStatusNot(
            Long accountId,
            String providerCode,
            PaymentAccountBindingStatus excludedStatus
    );

    Optional<PaymentAccountBinding> findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
            Long accountId,
            String providerCode,
            String commandIdempotencyKey
    );

    Optional<PaymentAccountBinding> findFirstByAccountIdAndProviderCodeAndBindingStatus(
            Long accountId,
            String providerCode,
            PaymentAccountBindingStatus status
    );
}

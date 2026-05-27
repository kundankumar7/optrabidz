package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.PaymentAttempt;
import com.project.optrabidz.financial.domain.repository.PaymentAttemptRepository;
import com.project.optrabidz.financial.infrastructure.mapper.FinancialPersistenceMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class PaymentAttemptRepositoryAdapter implements PaymentAttemptRepository {
    private final JpaPaymentAttemptRepository jpaPaymentAttemptRepository;
    private final FinancialPersistenceMapper mapper;

    public PaymentAttemptRepositoryAdapter(JpaPaymentAttemptRepository jpaPaymentAttemptRepository,
                                           FinancialPersistenceMapper mapper) {
        this.jpaPaymentAttemptRepository = jpaPaymentAttemptRepository;
        this.mapper = mapper;
    }

    @Override
    public PaymentAttempt save(PaymentAttempt paymentAttempt) {
        return mapper.toDomain(jpaPaymentAttemptRepository.save(mapper.toEntity(paymentAttempt)));
    }

    @Override
    public Optional<PaymentAttempt> findById(Long paymentAttemptId) {
        return jpaPaymentAttemptRepository.findById(paymentAttemptId)
                .map(mapper::toDomain);
    }

    @Override
    public int confirmActive(Long paymentAttemptId, String providerPaymentId, Instant now) {
        return jpaPaymentAttemptRepository.confirmActive(paymentAttemptId, providerPaymentId, now);
    }

    @Override
    public int failActive(Long paymentAttemptId, String failureCode, String failureMessage, Instant now) {
        return jpaPaymentAttemptRepository.failActive(paymentAttemptId, failureCode, failureMessage, now);
    }
}

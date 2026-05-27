package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.infrastructure.entity.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPaymentProviderRepository extends JpaRepository<PaymentProvider, String> {
}

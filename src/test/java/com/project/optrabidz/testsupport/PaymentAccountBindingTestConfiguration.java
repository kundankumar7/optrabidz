package com.project.optrabidz.testsupport;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.financial.api.PaymentAccountBindingController;
import com.project.optrabidz.financial.application.PaymentAccountBindingService;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.financial.infrastructure.adapter.DemoReceivingAccountReadinessAdapter;
import com.project.optrabidz.marketplace.application.port.ReceivingAccountReadinessPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test & !demo")
public class PaymentAccountBindingTestConfiguration {

    @Bean
    PaymentAccountBindingService paymentAccountBindingService(
            PaymentAccountBindingRepository repository,
            EventPublisher eventPublisher
    ) {
        return new PaymentAccountBindingService(repository, eventPublisher);
    }

    @Bean
    PaymentAccountBindingController paymentAccountBindingController(
            PaymentAccountBindingService service
    ) {
        return new PaymentAccountBindingController(service);
    }

    @Bean
    @Primary
    ReceivingAccountReadinessPort paymentAccountBindingReadinessPort(
            PaymentAccountBindingRepository repository
    ) {
        return new DemoReceivingAccountReadinessAdapter(repository);
    }
}

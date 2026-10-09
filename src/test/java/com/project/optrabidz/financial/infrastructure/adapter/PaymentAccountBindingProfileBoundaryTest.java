package com.project.optrabidz.financial.infrastructure.adapter;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.financial.api.PaymentAccountBindingController;
import com.project.optrabidz.financial.application.PaymentAccountBindingService;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.marketplace.application.port.ReceivingAccountReadinessPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PaymentAccountBindingProfileBoundaryTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BindingConfiguration.class)
            .withBean(PaymentAccountBindingRepository.class,
                    () -> mock(PaymentAccountBindingRepository.class))
            .withBean(EventPublisher.class, () -> mock(EventPublisher.class));

    @Test
    void productionUsesFailClosedReadinessWithoutDemoBindingMutationComponents() {
        contextRunner.withSystemProperties("spring.profiles.active=prod")
                .withPropertyValues("optrabidz.financial.demo-provider.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .doesNotHaveBean(PaymentAccountBindingController.class)
                            .doesNotHaveBean(PaymentAccountBindingService.class)
                            .doesNotHaveBean(DemoReceivingAccountReadinessAdapter.class)
                            .hasSingleBean(ReceivingAccountReadinessPort.class);
                    assertThat(context.getBean(ReceivingAccountReadinessPort.class)
                            .requiresVerifiedBinding()).isFalse();
                    assertThat(context.getBean(ReceivingAccountReadinessPort.class)
                            .hasVerifiedBinding(501L)).isFalse();
                });
    }

    @Test
    void demonstrationProfileEnablesTheDemoBindingFlowExplicitly() {
        contextRunner.withSystemProperties("spring.profiles.active=demo")
                .withPropertyValues("optrabidz.financial.demo-provider.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .hasSingleBean(PaymentAccountBindingController.class)
                            .hasSingleBean(PaymentAccountBindingService.class)
                            .hasSingleBean(DemoReceivingAccountReadinessAdapter.class)
                            .hasSingleBean(ReceivingAccountReadinessPort.class);
                    assertThat(context.getBean(ReceivingAccountReadinessPort.class))
                            .isInstanceOf(DemoReceivingAccountReadinessAdapter.class);
                    assertThat(context.getBean(ReceivingAccountReadinessPort.class)
                            .requiresVerifiedBinding()).isTrue();
                });
    }

    @Test
    void disabledDemoProviderFallsBackToNotReady() {
        contextRunner.withSystemProperties("spring.profiles.active=demo")
                .withPropertyValues("optrabidz.financial.demo-provider.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .doesNotHaveBean(PaymentAccountBindingController.class)
                            .doesNotHaveBean(PaymentAccountBindingService.class)
                            .doesNotHaveBean(DemoReceivingAccountReadinessAdapter.class)
                            .hasSingleBean(ReceivingAccountReadinessPort.class);
                    assertThat(context.getBean(ReceivingAccountReadinessPort.class)
                            .requiresVerifiedBinding()).isFalse();
                    assertThat(context.getBean(ReceivingAccountReadinessPort.class)
                            .hasVerifiedBinding(601L)).isFalse();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
            basePackages = {
                    "com.project.optrabidz.financial.api",
                    "com.project.optrabidz.financial.application",
                    "com.project.optrabidz.financial.infrastructure.adapter"
            },
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = {
                            "com\\.project\\.optrabidz\\.financial\\.api\\.PaymentAccountBindingController",
                            "com\\.project\\.optrabidz\\.financial\\.application\\.PaymentAccountBindingService",
                            "com\\.project\\.optrabidz\\.financial\\.infrastructure\\.adapter\\..*ReceivingAccountReadinessAdapter"
                    }
            )
    )
    static class BindingConfiguration {
    }
}

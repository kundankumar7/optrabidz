package com.project.optrabidz.financial.api;

import com.project.optrabidz.financial.application.DemoPaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DemoPaymentControllerProfileTest {
    @Test
    void controllerExistsOnlyWhenDemoProfileAndEnablePropertyAreBothPresent() {
        context("demo", true).run(context ->
                assertThat(context).hasSingleBean(DemoPaymentController.class));
        context("test", true).run(context ->
                assertThat(context).doesNotHaveBean(DemoPaymentController.class));
        context("demo", false).run(context ->
                assertThat(context).doesNotHaveBean(DemoPaymentController.class));
    }

    private ApplicationContextRunner context(String profile, boolean enabled) {
        return new ApplicationContextRunner()
                .withInitializer(context -> ((ConfigurableEnvironment) context.getEnvironment())
                        .setActiveProfiles(profile))
                .withPropertyValues("optrabidz.financial.demo-provider.enabled=" + enabled)
                .withUserConfiguration(MockServiceConfiguration.class, DemoPaymentController.class);
    }

    @Configuration(proxyBeanMethods = false)
    static class MockServiceConfiguration {
        @Bean
        DemoPaymentService demoPaymentService() {
            return mock(DemoPaymentService.class);
        }
    }
}

package com.project.optrabidz.financial.infrastructure.provider;

import com.project.optrabidz.financial.application.strategy.LocalPaymentStrategy;
import com.project.optrabidz.financial.infrastructure.provider.sandbox.SandboxUpiPaymentStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProviderProfileBoundaryTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProviderConfiguration.class)
            .withPropertyValues(
                    "optrabidz.financial.local-provider.enabled=true",
                    "optrabidz.financial.sandbox-providers.enabled=true"
            );

    @Test
    void enabledDevelopmentProvidersAreUnavailableWithoutAnExplicitProfile() {
        contextRunner.withSystemProperties("spring.profiles.active=")
                .run(context -> assertThat(context)
                .doesNotHaveBean(LocalPaymentStrategy.class)
                .doesNotHaveBean(SandboxUpiPaymentStrategy.class));
    }

    @Test
    void enabledDevelopmentProvidersAreUnavailableInProduction() {
        contextRunner.withSystemProperties("spring.profiles.active=prod")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(LocalPaymentStrategy.class)
                        .doesNotHaveBean(SandboxUpiPaymentStrategy.class));
    }

    @Test
    void enabledDevelopmentProvidersAreAvailableInDevelopment() {
        contextRunner.withSystemProperties("spring.profiles.active=dev")
                .run(context -> assertThat(context)
                        .hasSingleBean(LocalPaymentStrategy.class)
                        .hasSingleBean(SandboxUpiPaymentStrategy.class));
    }

    @Test
    void packagedConfigurationDoesNotActivateDevelopmentProfile() throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = getClass().getResourceAsStream("/application.properties")) {
            assertThat(stream).isNotNull();
            properties.load(stream);
        }

        assertThat(properties).doesNotContainKey("spring.profiles.active");
    }

    @Configuration(proxyBeanMethods = false)
    @Import({LocalPaymentStrategy.class, SandboxUpiPaymentStrategy.class})
    static class ProviderConfiguration {
    }
}

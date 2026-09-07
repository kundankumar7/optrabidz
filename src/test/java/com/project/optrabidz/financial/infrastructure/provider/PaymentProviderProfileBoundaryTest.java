package com.project.optrabidz.financial.infrastructure.provider;

import com.project.optrabidz.financial.application.strategy.LocalPaymentStrategy;
import com.project.optrabidz.financial.infrastructure.provider.sandbox.SandboxUpiPaymentStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProviderProfileBoundaryTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProviderConfiguration.class)
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class));

    @Test
    void disabledDevelopmentProvidersRequireNoApprovedProfile() {
        contextRunner.withSystemProperties("spring.profiles.active=prod")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(LocalPaymentStrategy.class)
                        .doesNotHaveBean(SandboxUpiPaymentStrategy.class));
    }

    @Test
    void enabledLocalProviderFailsWithoutAnExplicitProfile() {
        contextRunner.withSystemProperties("spring.profiles.active=")
                .withPropertyValues("optrabidz.financial.local-provider.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void enabledSandboxProvidersFailInProduction() {
        contextRunner.withSystemProperties("spring.profiles.active=prod")
                .withPropertyValues("optrabidz.financial.sandbox-providers.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void enabledDevelopmentProvidersAreAvailableInDevelopment() {
        contextRunner.withSystemProperties("spring.profiles.active=dev")
                .withPropertyValues(enabledProviderProperties())
                .run(context -> assertThat(context)
                        .hasSingleBean(LocalPaymentStrategy.class)
                        .hasSingleBean(SandboxUpiPaymentStrategy.class));
    }

    @Test
    void enabledDevelopmentProvidersAreAvailableInTests() {
        contextRunner.withSystemProperties("spring.profiles.active=test")
                .withPropertyValues(enabledProviderProperties())
                .run(context -> assertThat(context)
                        .hasSingleBean(LocalPaymentStrategy.class)
                        .hasSingleBean(SandboxUpiPaymentStrategy.class));
    }

    @Test
    void productionProfileCannotBeBypassedByAlsoActivatingDevelopment() {
        contextRunner.withSystemProperties("spring.profiles.active=prod,dev")
                .withPropertyValues(enabledProviderProperties())
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()))
                            .hasMessageContaining("local")
                            .hasMessageContaining("sandbox")
                            .hasMessageContaining("prod");
                });
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

    private static String[] enabledProviderProperties() {
        return new String[] {
                "optrabidz.financial.local-provider.enabled=true",
                "optrabidz.financial.sandbox-providers.enabled=true"
        };
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Configuration(proxyBeanMethods = false)
    @Import({LocalPaymentStrategy.class, SandboxUpiPaymentStrategy.class})
    @ComponentScan(
            basePackages = "com.project.optrabidz.financial.infrastructure.provider",
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = {
                            "com\\.project\\.optrabidz\\.financial\\.infrastructure\\.provider\\.DevelopmentPaymentProviderProperties",
                            "com\\.project\\.optrabidz\\.financial\\.infrastructure\\.provider\\.DevelopmentPaymentProviderConfigurationPolicy"
                    }
            )
    )
    static class ProviderConfiguration {
    }
}

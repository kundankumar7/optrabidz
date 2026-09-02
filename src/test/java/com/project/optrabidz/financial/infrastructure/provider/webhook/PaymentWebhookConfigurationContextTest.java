package com.project.optrabidz.financial.infrastructure.provider.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentWebhookConfigurationContextTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(WebhookConfiguration.class)
            .withSystemProperties("spring.profiles.active=prod");

    @Test
    void disabledProviderStartsWithoutSecretMaterial() {
        contextRunner.withPropertyValues("optrabidz.financial.webhook.providers.UPI.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void enabledProviderWithoutSecretFailsBeforeReadiness() {
        contextRunner.withPropertyValues("optrabidz.financial.webhook.providers.UPI.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void invalidConfiguredSecretIsNotDisclosedByStartupFailure() {
        String restrictedSecret = "dev-only-private-webhook-secret-material-001";

        contextRunner.withPropertyValues(
                        "optrabidz.financial.webhook.providers.UPI.enabled=true",
                        "optrabidz.financial.webhook.providers.UPI.active-secret=" + restrictedSecret
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()))
                            .hasMessageContaining("UPI")
                            .hasMessageNotContaining(restrictedSecret);
                });
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Configuration(proxyBeanMethods = false)
    @Import({PaymentWebhookProperties.class, PaymentWebhookConfigurationPolicy.class})
    static class WebhookConfiguration {
    }
}

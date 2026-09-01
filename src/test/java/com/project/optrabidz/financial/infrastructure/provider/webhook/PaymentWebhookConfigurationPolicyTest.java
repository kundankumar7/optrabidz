package com.project.optrabidz.financial.infrastructure.provider.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentWebhookConfigurationPolicyTest {
    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String STRONG_SECRET = "production-webhook-secret-material-0001";

    @Test
    void disabledProviderDoesNotRequireSecretMaterial() {
        PaymentWebhookProperties properties = properties(provider(false, null, null, null));

        assertThatCode(() -> validate(properties, "prod")).doesNotThrowAnyException();
    }

    @Test
    void enabledProviderRequiresActiveSecret() {
        PaymentWebhookProperties properties = properties(provider(true, null, null, null));

        assertThatThrownBy(() -> validate(properties, "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UPI")
                .hasMessageNotContaining(STRONG_SECRET);
    }

    @Test
    void enabledProviderRejectsShortSecret() {
        PaymentWebhookProperties properties = properties(provider(true, "too-short", null, null));

        assertThatThrownBy(() -> validate(properties, "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UPI")
                .hasMessageNotContaining("too-short");
    }

    @Test
    void previousSecretRequiresFutureExpiry() {
        PaymentWebhookProperties missingExpiry = properties(provider(
                true, STRONG_SECRET, "previous-webhook-secret-material-001", null));
        PaymentWebhookProperties expired = properties(provider(
                true, STRONG_SECRET, "previous-webhook-secret-material-001", NOW));

        assertThatThrownBy(() -> validate(missingExpiry, "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UPI");
        assertThatThrownBy(() -> validate(expired, "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UPI");
    }

    @Test
    void validActiveAndPreviousSecretsPassValidation() {
        PaymentWebhookProperties properties = properties(provider(
                true,
                STRONG_SECRET,
                "previous-webhook-secret-material-001",
                NOW.plusSeconds(3600)
        ));

        assertThatCode(() -> validate(properties, "prod")).doesNotThrowAnyException();
    }

    @Test
    void developmentMarkerSecretIsRejectedOutsideDevelopmentOrTest() {
        String developmentSecret = "dev-only-upi-webhook-secret-material-001";
        PaymentWebhookProperties properties = properties(provider(true, developmentSecret, null, null));

        assertThatThrownBy(() -> validate(properties, "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UPI")
                .hasMessageNotContaining(developmentSecret);
        assertThatCode(() -> validate(properties, "dev")).doesNotThrowAnyException();
        assertThatCode(() -> validate(properties, "test")).doesNotThrowAnyException();
    }

    private static PaymentWebhookProperties properties(
            PaymentWebhookProperties.ProviderConfiguration provider) {
        PaymentWebhookProperties properties = new PaymentWebhookProperties();
        properties.setProviders(Map.of("upi", provider));
        return properties;
    }

    private static PaymentWebhookProperties.ProviderConfiguration provider(
            boolean enabled,
            String activeSecret,
            String previousSecret,
            Instant previousSecretValidUntil) {
        PaymentWebhookProperties.ProviderConfiguration provider =
                new PaymentWebhookProperties.ProviderConfiguration();
        provider.setEnabled(enabled);
        provider.setActiveSecret(activeSecret);
        provider.setPreviousSecret(previousSecret);
        provider.setPreviousSecretValidUntil(previousSecretValidUntil);
        return provider;
    }

    private static void validate(PaymentWebhookProperties properties, String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        new PaymentWebhookConfigurationPolicy(properties, environment, CLOCK)
                .afterSingletonsInstantiated();
    }
}

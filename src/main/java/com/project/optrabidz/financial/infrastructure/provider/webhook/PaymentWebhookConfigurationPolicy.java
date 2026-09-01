package com.project.optrabidz.financial.infrastructure.provider.webhook;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

@Component
public class PaymentWebhookConfigurationPolicy implements SmartInitializingSingleton {
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final int MAXIMUM_SECRET_BYTES = 512;

    private final PaymentWebhookProperties properties;
    private final Environment environment;
    private final Clock clock;

    @Autowired
    public PaymentWebhookConfigurationPolicy(PaymentWebhookProperties properties,
                                             Environment environment) {
        this(properties, environment, Clock.systemUTC());
    }

    PaymentWebhookConfigurationPolicy(PaymentWebhookProperties properties,
                                      Environment environment,
                                      Clock clock) {
        this.properties = properties;
        this.environment = environment;
        this.clock = clock;
    }

    @Override
    public void afterSingletonsInstantiated() {
        validateGlobalLimits();
        boolean developmentEnvironment = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("dev") || profile.equals("test"));
        properties.getProviders().forEach((providerCode, provider) ->
                validateProvider(providerCode, provider, developmentEnvironment));
    }

    private void validateGlobalLimits() {
        if (properties.getMaxBodySize() == null || properties.getMaxBodySize().toBytes() <= 0) {
            throw new IllegalStateException("Webhook max body size must be positive");
        }
        if (properties.getTimestampTolerance() == null
                || properties.getTimestampTolerance().isZero()
                || properties.getTimestampTolerance().isNegative()) {
            throw new IllegalStateException("Webhook timestamp tolerance must be positive");
        }
    }

    private void validateProvider(String providerCode,
                                  PaymentWebhookProperties.ProviderConfiguration provider,
                                  boolean developmentEnvironment) {
        if (provider == null || !provider.isEnabled()) {
            return;
        }
        validateSecret(providerCode, "active", provider.getActiveSecret(), developmentEnvironment);

        String previousSecret = provider.getPreviousSecret();
        Instant previousExpiry = provider.getPreviousSecretValidUntil();
        if (previousSecret == null || previousSecret.isBlank()) {
            if (previousExpiry != null) {
                throw invalid(providerCode, "previous secret expiry requires previous secret material");
            }
            return;
        }

        validateSecret(providerCode, "previous", previousSecret, developmentEnvironment);
        if (previousSecret.equals(provider.getActiveSecret())) {
            throw invalid(providerCode, "active and previous secrets must differ");
        }
        if (previousExpiry == null || !previousExpiry.isAfter(clock.instant())) {
            throw invalid(providerCode, "previous secret requires a future expiry");
        }
    }

    private void validateSecret(String providerCode,
                                String label,
                                String secret,
                                boolean developmentEnvironment) {
        if (secret == null || secret.isBlank()) {
            throw invalid(providerCode, label + " secret is required");
        }
        if (!secret.equals(secret.strip())) {
            throw invalid(providerCode, label + " secret has surrounding whitespace");
        }
        int byteLength = secret.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength < MINIMUM_SECRET_BYTES || byteLength > MAXIMUM_SECRET_BYTES) {
            throw invalid(providerCode, label + " secret length is unacceptable");
        }
        String normalized = secret.toLowerCase(Locale.ROOT);
        if (!developmentEnvironment
                && (normalized.startsWith("dev-only-") || normalized.startsWith("test-only-"))) {
            throw invalid(providerCode, label + " secret is restricted to development or test");
        }
    }

    private IllegalStateException invalid(String providerCode, String reason) {
        return new IllegalStateException(
                "Invalid webhook configuration for provider " + safeProviderCode(providerCode) + ": " + reason
        );
    }

    private String safeProviderCode(String providerCode) {
        if (providerCode == null || !providerCode.matches("[A-Z0-9_-]{1,32}")) {
            return "UNKNOWN";
        }
        return providerCode;
    }
}

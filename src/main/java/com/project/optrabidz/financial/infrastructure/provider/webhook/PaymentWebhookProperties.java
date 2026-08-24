package com.project.optrabidz.financial.infrastructure.provider.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@Validated
@ConfigurationProperties(prefix = "optrabidz.financial.webhook")
public class PaymentWebhookProperties {
    private DataSize maxBodySize = DataSize.ofKilobytes(64);
    private Duration timestampTolerance = Duration.ofMinutes(5);
    private Map<String, ProviderConfiguration> providers = new LinkedHashMap<>();

    public Optional<ProviderConfiguration> enabledProvider(String providerCode) {
        ProviderConfiguration provider = providers.get(normalize(providerCode));
        return provider != null && provider.isEnabled()
                ? Optional.of(provider)
                : Optional.empty();
    }

    public DataSize getMaxBodySize() {
        return maxBodySize;
    }

    public void setMaxBodySize(DataSize maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public Duration getTimestampTolerance() {
        return timestampTolerance;
    }

    public void setTimestampTolerance(Duration timestampTolerance) {
        this.timestampTolerance = timestampTolerance;
    }

    public Map<String, ProviderConfiguration> getProviders() {
        return Map.copyOf(providers);
    }

    public void setProviders(Map<String, ProviderConfiguration> providers) {
        Map<String, ProviderConfiguration> normalized = new LinkedHashMap<>();
        if (providers != null) {
            providers.forEach((providerCode, configuration) ->
                    normalized.put(normalize(providerCode), configuration));
        }
        this.providers = normalized;
    }

    public Optional<String> secretForProvider(String providerCode) {
        return enabledProvider(providerCode)
                .map(ProviderConfiguration::getActiveSecret)
                .filter(secret -> !secret.isBlank());
    }

    public void setHmacSecrets(Map<String, String> hmacSecrets) {
        Map<String, ProviderConfiguration> compatibilityProviders = new HashMap<>();
        if (hmacSecrets != null) {
            hmacSecrets.forEach((providerCode, secret) -> {
                ProviderConfiguration provider = new ProviderConfiguration();
                provider.setEnabled(true);
                provider.setActiveSecret(secret);
                compatibilityProviders.put(providerCode, provider);
            });
        }
        setProviders(compatibilityProviders);
    }

    private static String normalize(String providerCode) {
        return providerCode == null
                ? ""
                : providerCode.strip().toUpperCase(Locale.ROOT);
    }

    public static final class ProviderConfiguration {
        private boolean enabled;
        private String activeSecret;
        private String previousSecret;
        private Instant previousSecretValidUntil;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getActiveSecret() {
            return activeSecret;
        }

        public void setActiveSecret(String activeSecret) {
            this.activeSecret = activeSecret;
        }

        public String getPreviousSecret() {
            return previousSecret;
        }

        public void setPreviousSecret(String previousSecret) {
            this.previousSecret = previousSecret;
        }

        public Instant getPreviousSecretValidUntil() {
            return previousSecretValidUntil;
        }

        public void setPreviousSecretValidUntil(Instant previousSecretValidUntil) {
            this.previousSecretValidUntil = previousSecretValidUntil;
        }
    }
}

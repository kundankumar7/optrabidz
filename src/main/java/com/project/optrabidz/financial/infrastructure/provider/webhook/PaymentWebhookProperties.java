package com.project.optrabidz.financial.infrastructure.provider.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "optrabidz.financial.webhook")
public class PaymentWebhookProperties {
    private Map<String, String> hmacSecrets = new HashMap<>();

    public Optional<String> secretForProvider(String providerCode) {
        if (providerCode == null) {
            return Optional.empty();
        }
        String secret = hmacSecrets.get(providerCode.trim().toUpperCase());
        if (secret == null || secret.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(secret.trim());
    }

    public Map<String, String> getHmacSecrets() {
        return Map.copyOf(hmacSecrets);
    }

    public void setHmacSecrets(Map<String, String> hmacSecrets) {
        Map<String, String> normalizedSecrets = new HashMap<>();
        if (hmacSecrets != null) {
            hmacSecrets.forEach((providerCode, secret) -> {
                if (providerCode != null && secret != null && !secret.isBlank()) {
                    normalizedSecrets.put(providerCode.trim().toUpperCase(), secret.trim());
                }
            });
        }
        this.hmacSecrets = normalizedSecrets;
    }
}

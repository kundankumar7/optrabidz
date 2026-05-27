package com.project.optrabidz.common.observability;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class SensitiveDataMasker {
    private static final String MASK = "****";
    private static final List<ReplacementRule> SENSITIVE_RULES = List.of(
            new ReplacementRule(
                    Pattern.compile("(?i)(\"(?:password|csrf|token|secret|cookie|authorization|apiKey|providerSecret)\"\\s*:\\s*\")([^\"]*)(\")"),
                    "$1" + MASK + "$3"
            ),
            new ReplacementRule(
                    Pattern.compile("(?i)((?:password|csrf|token|secret|cookie|authorization|apiKey|providerSecret)=)([^\\s&]+)"),
                    "$1" + MASK
            ),
            new ReplacementRule(
                    Pattern.compile("(?i)((?:authorization|cookie|x-csrf-token|x-xsrf-token)\\s*:\\s*)([^\\r\\n]+)"),
                    "$1" + MASK
            ),
            new ReplacementRule(
                    Pattern.compile("(?i)(bearer\\s+)([A-Za-z0-9._~+/=-]+)"),
                    "$1" + MASK
            )
    );

    public String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String masked = value;
        for (ReplacementRule rule : SENSITIVE_RULES) {
            masked = rule.pattern().matcher(masked).replaceAll(rule.replacement());
        }
        return masked;
    }

    public String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@", 2);
        String local = parts[0];
        if (local.length() <= 2) {
            return MASK + "@" + parts[1];
        }
        return local.charAt(0) + MASK + local.charAt(local.length() - 1) + "@" + parts[1];
    }

    private record ReplacementRule(Pattern pattern, String replacement) {
    }
}

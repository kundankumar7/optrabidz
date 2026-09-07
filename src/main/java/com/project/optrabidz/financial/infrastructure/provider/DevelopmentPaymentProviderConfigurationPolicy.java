package com.project.optrabidz.financial.infrastructure.provider;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DevelopmentPaymentProviderConfigurationPolicy implements SmartInitializingSingleton {
    private final DevelopmentPaymentProviderProperties properties;
    private final Environment environment;

    public DevelopmentPaymentProviderConfigurationPolicy(
            DevelopmentPaymentProviderProperties properties,
            Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<String> enabledProviders = enabledProviders();
        if (enabledProviders.isEmpty()) {
            return;
        }

        String[] activeProfiles = environment.getActiveProfiles();
        boolean approvedProfiles = activeProfiles.length > 0
                && Arrays.stream(activeProfiles).allMatch(this::isDevelopmentOrTest);
        if (!approvedProfiles) {
            throw new IllegalStateException(
                    "Development payment providers " + String.join(" and ", enabledProviders)
                            + " require only dev or test profiles; prod and other profiles are not permitted"
            );
        }
    }

    private List<String> enabledProviders() {
        List<String> enabled = new ArrayList<>(2);
        if (properties.getLocalProvider().isEnabled()) {
            enabled.add("local");
        }
        if (properties.getSandboxProviders().isEnabled()) {
            enabled.add("sandbox");
        }
        return enabled;
    }

    private boolean isDevelopmentOrTest(String profile) {
        return "dev".equals(profile) || "test".equals(profile);
    }
}

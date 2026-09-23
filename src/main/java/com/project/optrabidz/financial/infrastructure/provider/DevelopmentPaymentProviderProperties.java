package com.project.optrabidz.financial.infrastructure.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "optrabidz.financial")
public class DevelopmentPaymentProviderProperties {
    /** Local payment provider settings. */
    private final ProviderSwitch localProvider = new ProviderSwitch();

    /** Sandbox payment provider settings. */
    private final ProviderSwitch sandboxProviders = new ProviderSwitch();

    public ProviderSwitch getLocalProvider() {
        return localProvider;
    }

    public ProviderSwitch getSandboxProviders() {
        return sandboxProviders;
    }

    public static final class ProviderSwitch {
        /** Whether the provider is enabled. */
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

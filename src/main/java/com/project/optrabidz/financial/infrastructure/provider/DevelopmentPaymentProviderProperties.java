package com.project.optrabidz.financial.infrastructure.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "optrabidz.financial")
public class DevelopmentPaymentProviderProperties {
    private final ProviderSwitch localProvider = new ProviderSwitch();
    private final ProviderSwitch sandboxProviders = new ProviderSwitch();

    public ProviderSwitch getLocalProvider() {
        return localProvider;
    }

    public ProviderSwitch getSandboxProviders() {
        return sandboxProviders;
    }

    public static final class ProviderSwitch {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

package com.project.optrabidz.governance.application.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "optrabidz.admin.recovery")
public class AdminRecoveryProperties {
    /** Whether the administrator recovery endpoint is enabled. */
    private boolean enabled;

    /** Private token required by the administrator recovery endpoint. */
    private String token;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

package com.project.optrabidz.governance.application.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "optrabidz.admin.bootstrap")
public class AdminBootstrapProperties {
    private boolean enabled;
    private boolean recoveryMode;
    private String email;
    private String password;
    private String publicDisplayName;
    private String organizationLabel;
    private String recoveryToken;

    public BootstrapAdminCommand toBootstrapCommand() {
        return new BootstrapAdminCommand(email, password, publicDisplayName, organizationLabel);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRecoveryMode() {
        return recoveryMode;
    }

    public void setRecoveryMode(boolean recoveryMode) {
        this.recoveryMode = recoveryMode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPublicDisplayName() {
        return publicDisplayName;
    }

    public void setPublicDisplayName(String publicDisplayName) {
        this.publicDisplayName = publicDisplayName;
    }

    public String getOrganizationLabel() {
        return organizationLabel;
    }

    public void setOrganizationLabel(String organizationLabel) {
        this.organizationLabel = organizationLabel;
    }

    public String getRecoveryToken() {
        return recoveryToken;
    }

    public void setRecoveryToken(String recoveryToken) {
        this.recoveryToken = recoveryToken;
    }
}

package com.project.optrabidz.governance.application.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "optrabidz.admin.bootstrap")
public class AdminBootstrapProperties {
    /** Whether administrator bootstrapping is enabled. */
    private boolean enabled;

    /** Email address assigned to the bootstrap administrator. */
    private String email;

    /** Initial password for the bootstrap administrator. */
    private String password;

    /** Public display name assigned to the bootstrap administrator. */
    private String publicDisplayName;

    /** Organization label assigned to the bootstrap administrator. */
    private String organizationLabel;

    public BootstrapAdminCommand toBootstrapCommand() {
        return new BootstrapAdminCommand(email, password, publicDisplayName, organizationLabel);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
}

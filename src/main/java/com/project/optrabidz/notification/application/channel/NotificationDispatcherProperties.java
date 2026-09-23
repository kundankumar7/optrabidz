package com.project.optrabidz.notification.application.channel;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "optrabidz.notification.dispatcher")
public class NotificationDispatcherProperties {
    /** Whether scheduled notification delivery dispatching is enabled. */
    private boolean enabled = true;

    /** Initial delay in milliseconds before scheduled notification dispatching starts. */
    private long initialDelayMs = 5000;

    /** Delay in milliseconds between scheduled notification dispatch runs. */
    private long fixedDelayMs = 5000;

    /** Maximum number of notification deliveries processed in one dispatch run. */
    private int batchSize = 50;

    /** Maximum number of delivery attempts before a notification delivery becomes final. */
    private int maxAttempts = 3;

    /** Worker identifier recorded while a notification delivery is being processed. */
    private String workerId = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public int getBatchSize() {
        return Math.max(batchSize, 1);
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return Math.max(maxAttempts, 1);
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
}

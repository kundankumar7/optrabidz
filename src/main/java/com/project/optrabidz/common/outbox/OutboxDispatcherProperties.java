package com.project.optrabidz.common.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "optrabidz.outbox.dispatcher")
public class OutboxDispatcherProperties {
    /** Whether scheduled outbox dispatching is enabled. */
    private boolean enabled = true;

    /** Initial delay in milliseconds before scheduled outbox dispatching starts. */
    private long initialDelayMs = 5000;

    /** Delay in milliseconds between scheduled outbox dispatch runs. */
    private long fixedDelayMs = 5000;

    /** Maximum number of outbox events processed in one dispatch run. */
    private int batchSize = 50;

    /** Worker identifier recorded while an outbox event is being processed. */
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

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
}

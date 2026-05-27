package com.project.optrabidz.governance.application.lifecycle;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LifecycleExpiryScheduler {
    private final LifecycleEnforcementService lifecycleEnforcementService;
    private final boolean enabled;

    public LifecycleExpiryScheduler(LifecycleEnforcementService lifecycleEnforcementService,
                                    @Value("${optrabidz.governance.lifecycle.scheduler.enabled:true}")
                                    boolean enabled) {
        this.lifecycleEnforcementService = lifecycleEnforcementService;
        this.enabled = enabled;
    }

    @Scheduled(
            initialDelayString = "${optrabidz.governance.lifecycle.scheduler.initial-delay-ms:60000}",
            fixedDelayString = "${optrabidz.governance.lifecycle.scheduler.fixed-delay-ms:300000}"
    )
    public void enforceExpiries() {
        if (!enabled) {
            return;
        }
        lifecycleEnforcementService.enforceDueLifecycleRules();
    }
}

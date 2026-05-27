package com.project.optrabidz.governance.application.lifecycle;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LifecycleEnforcementController {
    private final LifecycleEnforcementService lifecycleEnforcementService;

    public LifecycleEnforcementController(LifecycleEnforcementService lifecycleEnforcementService) {
        this.lifecycleEnforcementService = lifecycleEnforcementService;
    }

    public List<LifecycleEnforcementResult> enforceDueLifecycleRules() {
        return lifecycleEnforcementService.enforceDueLifecycleRules();
    }
}

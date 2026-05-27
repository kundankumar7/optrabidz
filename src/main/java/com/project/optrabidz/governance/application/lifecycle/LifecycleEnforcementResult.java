package com.project.optrabidz.governance.application.lifecycle;

import java.util.List;

public record LifecycleEnforcementResult(
        String ruleName,
        int evaluatedCount,
        int changedCount,
        int failedCount,
        List<String> messages
) {
    public LifecycleEnforcementResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static LifecycleEnforcementResult changed(String ruleName, int changedCount) {
        return new LifecycleEnforcementResult(ruleName, changedCount, changedCount, 0, List.of());
    }

    public static LifecycleEnforcementResult skipped(String ruleName, String reason) {
        return new LifecycleEnforcementResult(ruleName, 0, 0, 0, List.of(reason));
    }

    public static LifecycleEnforcementResult failed(String ruleName, String reason) {
        return new LifecycleEnforcementResult(ruleName, 0, 0, 1, List.of(reason));
    }
}

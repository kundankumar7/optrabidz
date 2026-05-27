package com.project.optrabidz.governance.application.lifecycle;

import java.time.Instant;

public interface LifecycleRule {
    String ruleName();

    LifecycleEnforcementResult enforce(Instant now);
}

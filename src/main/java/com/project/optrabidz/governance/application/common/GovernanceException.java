package com.project.optrabidz.governance.application.common;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.governance.application.error.GovernanceErrors;

import java.util.Objects;

public final class GovernanceException extends ApplicationException {
    public GovernanceException(GovernanceDecision decision) {
        super(
                GovernanceErrors.forRule(requireDenied(decision).code()),
                "GOVERNANCE." + requireDenied(decision).code().name(),
                requireDenied(decision).message()
        );
    }

    private static GovernanceDecision requireDenied(GovernanceDecision decision) {
        Objects.requireNonNull(decision, "decision must not be null");
        if (decision.allowed()) {
            throw new IllegalArgumentException(
                    "allowed decision cannot create a governance exception"
            );
        }
        return decision;
    }
}

package com.project.optrabidz.governance.application.common;

import java.util.List;

public record GovernanceDecision(
        boolean allowed,
        GovernanceRuleCode code,
        String message,
        List<GovernanceViolation> violations
) {
    public GovernanceDecision {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public static GovernanceDecision allow(String message) {
        return new GovernanceDecision(true, GovernanceRuleCode.ALLOWED, message, List.of());
    }

    public static GovernanceDecision deny(GovernanceRuleCode code, String context, String message) {
        return new GovernanceDecision(
                false,
                code,
                message,
                List.of(new GovernanceViolation(code, context, message))
        );
    }

    public static GovernanceDecision fromViolations(String successMessage,
                                                    String failureMessage,
                                                    List<GovernanceViolation> violations) {
        if (violations == null || violations.isEmpty()) {
            return allow(successMessage);
        }
        return new GovernanceDecision(false, violations.get(0).ruleCode(), failureMessage, violations);
    }
}

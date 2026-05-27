package com.project.optrabidz.governance.application.common;

public record GovernanceViolation(
        GovernanceRuleCode ruleCode,
        String context,
        String message
) {
}

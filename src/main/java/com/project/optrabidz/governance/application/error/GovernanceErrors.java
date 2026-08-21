package com.project.optrabidz.governance.application.error;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.governance.application.common.GovernanceRuleCode;

import java.util.Objects;

public final class GovernanceErrors {

    public static final ErrorDescriptor GOVERNANCE_ACTION_NOT_ELIGIBLE =
            new ErrorDescriptor(
                    "GOVERNANCE_ACTION_NOT_ELIGIBLE",
                    ErrorCategory.BUSINESS_RULE,
                    "The requested action does not satisfy governance eligibility rules"
            );

    public static final ErrorDescriptor GOVERNANCE_ACTION_NOT_PERMITTED =
            new ErrorDescriptor(
                    "GOVERNANCE_ACTION_NOT_PERMITTED",
                    ErrorCategory.AUTHORIZATION,
                    "The requested action is not permitted by governance policy"
            );

    public static final ErrorDescriptor GOVERNANCE_STATE_CONFLICT =
            new ErrorDescriptor(
                    "GOVERNANCE_STATE_CONFLICT",
                    ErrorCategory.CONFLICT,
                    "The requested action conflicts with the current governed state"
            );

    public static final ErrorDescriptor ADMIN_RECOVERY_ACCESS_DENIED =
            new ErrorDescriptor(
                    "ADMIN_RECOVERY_ACCESS_DENIED",
                    ErrorCategory.AUTHORIZATION,
                    "Admin recovery access was denied"
            );

    public static final ErrorDescriptor ADMIN_AUTHORITY_UNAVAILABLE =
            new ErrorDescriptor(
                    "ADMIN_AUTHORITY_UNAVAILABLE",
                    ErrorCategory.CONFLICT,
                    "No active administrator authority is available for transfer"
            );

    private GovernanceErrors() {
    }

    public static ErrorDescriptor forRule(GovernanceRuleCode ruleCode) {
        Objects.requireNonNull(ruleCode, "ruleCode must not be null");
        return switch (ruleCode) {
            case ACCOUNT_NOT_FOUND, ROLE_MISMATCH, ACCOUNT_NOT_ACTIVE,
                    PROFILE_INCOMPLETE, STARTUP_ACTOR_NOT_FOUND,
                    INVESTOR_ACTOR_NOT_FOUND, STARTUP_CLASSIFICATION_REQUIRED,
                    INVESTOR_PREFERENCE_REQUIRED -> GOVERNANCE_ACTION_NOT_ELIGIBLE;
            case ADMIN_AUTHORITY_REQUIRED, NEUTRALITY_VIOLATION,
                    SYSTEM_BOUNDARY_VIOLATION -> GOVERNANCE_ACTION_NOT_PERMITTED;
            case RECOVERY_MODE_REQUIRED -> ADMIN_RECOVERY_ACCESS_DENIED;
            case LIFECYCLE_RULE_SKIPPED, LIFECYCLE_RULE_FAILED ->
                    GOVERNANCE_STATE_CONFLICT;
            case ALLOWED -> throw new IllegalArgumentException(
                    "ALLOWED is not a governance failure"
            );
        };
    }
}

package com.project.optrabidz.governance.application;

import com.project.optrabidz.common.error.ErrorCategory;
import com.project.optrabidz.common.error.ErrorDescriptor;
import com.project.optrabidz.governance.application.common.GovernanceDecision;
import com.project.optrabidz.governance.application.common.GovernanceException;
import com.project.optrabidz.governance.application.common.GovernanceRuleCode;
import com.project.optrabidz.governance.application.error.GovernanceErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.ACCOUNT_NOT_ACTIVE;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.ACCOUNT_NOT_FOUND;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.ADMIN_AUTHORITY_REQUIRED;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.ALLOWED;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.INVESTOR_ACTOR_NOT_FOUND;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.INVESTOR_PREFERENCE_REQUIRED;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.LIFECYCLE_RULE_FAILED;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.LIFECYCLE_RULE_SKIPPED;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.NEUTRALITY_VIOLATION;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.PROFILE_INCOMPLETE;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.RECOVERY_MODE_REQUIRED;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.ROLE_MISMATCH;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.STARTUP_ACTOR_NOT_FOUND;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.STARTUP_CLASSIFICATION_REQUIRED;
import static com.project.optrabidz.governance.application.common.GovernanceRuleCode.SYSTEM_BOUNDARY_VIOLATION;
import static com.project.optrabidz.governance.application.error.GovernanceErrors.ADMIN_AUTHORITY_UNAVAILABLE;
import static com.project.optrabidz.governance.application.error.GovernanceErrors.ADMIN_RECOVERY_ACCESS_DENIED;
import static com.project.optrabidz.governance.application.error.GovernanceErrors.GOVERNANCE_ACTION_NOT_ELIGIBLE;
import static com.project.optrabidz.governance.application.error.GovernanceErrors.GOVERNANCE_ACTION_NOT_PERMITTED;
import static com.project.optrabidz.governance.application.error.GovernanceErrors.GOVERNANCE_STATE_CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class GovernanceErrorContractTest {

    @Test
    void exposesApprovedGroupedPublicDescriptors() {
        assertThat(GOVERNANCE_ACTION_NOT_ELIGIBLE).isEqualTo(descriptor(
                "GOVERNANCE_ACTION_NOT_ELIGIBLE",
                ErrorCategory.BUSINESS_RULE,
                "The requested action does not satisfy governance eligibility rules"
        ));
        assertThat(GOVERNANCE_ACTION_NOT_PERMITTED).isEqualTo(descriptor(
                "GOVERNANCE_ACTION_NOT_PERMITTED",
                ErrorCategory.AUTHORIZATION,
                "The requested action is not permitted by governance policy"
        ));
        assertThat(GOVERNANCE_STATE_CONFLICT).isEqualTo(descriptor(
                "GOVERNANCE_STATE_CONFLICT",
                ErrorCategory.CONFLICT,
                "The requested action conflicts with the current governed state"
        ));
        assertThat(ADMIN_RECOVERY_ACCESS_DENIED).isEqualTo(descriptor(
                "ADMIN_RECOVERY_ACCESS_DENIED",
                ErrorCategory.AUTHORIZATION,
                "Admin recovery access was denied"
        ));
        assertThat(ADMIN_AUTHORITY_UNAVAILABLE).isEqualTo(descriptor(
                "ADMIN_AUTHORITY_UNAVAILABLE",
                ErrorCategory.CONFLICT,
                "No active administrator authority is available for transfer"
        ));
    }

    @ParameterizedTest
    @MethodSource("deniedRuleMappings")
    void mapsEveryDeniedRuleToItsApprovedDescriptor(
            GovernanceRuleCode rule,
            ErrorDescriptor expected
    ) {
        assertThat(GovernanceErrors.forRule(rule)).isSameAs(expected);
    }

    @Test
    void rejectsAllowedRuleAndAllowedDecision() {
        assertThatThrownBy(() -> GovernanceErrors.forRule(ALLOWED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ALLOWED is not a governance failure");

        assertThatThrownBy(() -> new GovernanceException(
                GovernanceDecision.allow("Private success context")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("allowed decision cannot create a governance exception");
    }

    @Test
    void keepsInternalDecisionContextOutOfThePublicDescriptor() {
        GovernanceDecision decision = GovernanceDecision.deny(
                ROLE_MISMATCH,
                "private-role-context",
                "Expected STARTUP but found ADMIN"
        );

        GovernanceException failure = new GovernanceException(decision);

        assertThat(failure.descriptor()).isSameAs(GOVERNANCE_ACTION_NOT_ELIGIBLE);
        assertThat(failure.diagnosticCode()).isEqualTo("GOVERNANCE.ROLE_MISMATCH");
        assertThat(failure.getMessage()).contains("STARTUP", "ADMIN");
        assertThat(failure.descriptor().publicMessage())
                .doesNotContain("STARTUP", "ADMIN", "private-role-context", "ROLE_MISMATCH");
    }

    private static Stream<Arguments> deniedRuleMappings() {
        return Stream.of(
                arguments(ACCOUNT_NOT_FOUND, GOVERNANCE_ACTION_NOT_ELIGIBLE),
                arguments(ROLE_MISMATCH, GOVERNANCE_ACTION_NOT_ELIGIBLE),
                arguments(ACCOUNT_NOT_ACTIVE, GOVERNANCE_ACTION_NOT_ELIGIBLE),
                arguments(PROFILE_INCOMPLETE, GOVERNANCE_ACTION_NOT_ELIGIBLE),
                arguments(STARTUP_ACTOR_NOT_FOUND, GOVERNANCE_ACTION_NOT_ELIGIBLE),
                arguments(INVESTOR_ACTOR_NOT_FOUND, GOVERNANCE_ACTION_NOT_ELIGIBLE),
                arguments(STARTUP_CLASSIFICATION_REQUIRED, GOVERNANCE_ACTION_NOT_ELIGIBLE),
                arguments(INVESTOR_PREFERENCE_REQUIRED, GOVERNANCE_ACTION_NOT_ELIGIBLE),
                arguments(ADMIN_AUTHORITY_REQUIRED, GOVERNANCE_ACTION_NOT_PERMITTED),
                arguments(NEUTRALITY_VIOLATION, GOVERNANCE_ACTION_NOT_PERMITTED),
                arguments(SYSTEM_BOUNDARY_VIOLATION, GOVERNANCE_ACTION_NOT_PERMITTED),
                arguments(RECOVERY_MODE_REQUIRED, ADMIN_RECOVERY_ACCESS_DENIED),
                arguments(LIFECYCLE_RULE_SKIPPED, GOVERNANCE_STATE_CONFLICT),
                arguments(LIFECYCLE_RULE_FAILED, GOVERNANCE_STATE_CONFLICT)
        );
    }

    private static ErrorDescriptor descriptor(
            String code,
            ErrorCategory category,
            String publicMessage
    ) {
        return new ErrorDescriptor(code, category, publicMessage);
    }
}

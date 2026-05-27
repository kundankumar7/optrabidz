package com.project.optrabidz.governance.application.boundary;

import com.project.optrabidz.governance.application.common.GovernanceDecision;
import com.project.optrabidz.governance.application.common.GovernanceException;
import com.project.optrabidz.governance.application.common.GovernanceRuleCode;
import com.project.optrabidz.identity.domain.model.RoleType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class NeutralityGuard {
    public GovernanceDecision evaluateParticipantOnlyAction(RoleType actorRole, String actionName) {
        Assert.notNull(actorRole, "actorRole must not be null");
        Assert.hasText(actionName, "actionName must not be blank");

        if (actorRole == RoleType.ADMIN) {
            return GovernanceDecision.deny(
                    GovernanceRuleCode.NEUTRALITY_VIOLATION,
                    "role",
                    "Admin authority must not perform participant-owned action: " + actionName
            );
        }

        return GovernanceDecision.allow("Actor may perform participant-owned action: " + actionName);
    }

    public void assertParticipantOnlyAction(RoleType actorRole, String actionName) {
        GovernanceDecision decision = evaluateParticipantOnlyAction(actorRole, actionName);
        if (!decision.allowed()) {
            throw new GovernanceException(decision);
        }
    }
}

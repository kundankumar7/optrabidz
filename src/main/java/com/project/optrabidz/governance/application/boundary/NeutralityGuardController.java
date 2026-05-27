package com.project.optrabidz.governance.application.boundary;

import com.project.optrabidz.governance.application.common.GovernanceDecision;
import com.project.optrabidz.identity.domain.model.RoleType;
import org.springframework.stereotype.Service;

@Service
public class NeutralityGuardController {
    private final NeutralityGuard neutralityGuard;

    public NeutralityGuardController(NeutralityGuard neutralityGuard) {
        this.neutralityGuard = neutralityGuard;
    }

    public GovernanceDecision evaluateParticipantOnlyAction(RoleType actorRole, String actionName) {
        return neutralityGuard.evaluateParticipantOnlyAction(actorRole, actionName);
    }

    public void assertParticipantOnlyAction(RoleType actorRole, String actionName) {
        neutralityGuard.assertParticipantOnlyAction(actorRole, actionName);
    }
}

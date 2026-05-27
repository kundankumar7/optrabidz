package com.project.optrabidz.governance.application.boundary;

import com.project.optrabidz.governance.application.common.GovernanceDecision;
import com.project.optrabidz.governance.application.common.GovernanceException;
import com.project.optrabidz.governance.application.common.GovernanceRuleCode;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class SystemBoundaryGuard {
    public GovernanceDecision evaluateModuleMutation(String targetModule, boolean throughDeclaredPort) {
        Assert.hasText(targetModule, "targetModule must not be blank");

        if (!throughDeclaredPort) {
            return GovernanceDecision.deny(
                    GovernanceRuleCode.SYSTEM_BOUNDARY_VIOLATION,
                    targetModule,
                    "Governance must mutate " + targetModule + " through a declared command port"
            );
        }

        return GovernanceDecision.allow("Governance boundary is respected for " + targetModule);
    }

    public void assertMutationThroughDeclaredPort(String targetModule, boolean throughDeclaredPort) {
        GovernanceDecision decision = evaluateModuleMutation(targetModule, throughDeclaredPort);
        if (!decision.allowed()) {
            throw new GovernanceException(decision);
        }
    }
}

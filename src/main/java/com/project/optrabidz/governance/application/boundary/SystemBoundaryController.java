package com.project.optrabidz.governance.application.boundary;

import com.project.optrabidz.governance.application.common.GovernanceDecision;
import org.springframework.stereotype.Service;

@Service
public class SystemBoundaryController {
    private final SystemBoundaryGuard systemBoundaryGuard;

    public SystemBoundaryController(SystemBoundaryGuard systemBoundaryGuard) {
        this.systemBoundaryGuard = systemBoundaryGuard;
    }

    public GovernanceDecision evaluateModuleMutation(String targetModule, boolean throughDeclaredPort) {
        return systemBoundaryGuard.evaluateModuleMutation(targetModule, throughDeclaredPort);
    }

    public void assertMutationThroughDeclaredPort(String targetModule, boolean throughDeclaredPort) {
        systemBoundaryGuard.assertMutationThroughDeclaredPort(targetModule, throughDeclaredPort);
    }
}

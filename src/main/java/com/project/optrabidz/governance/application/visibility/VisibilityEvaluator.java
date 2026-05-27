package com.project.optrabidz.governance.application.visibility;

import com.project.optrabidz.governance.application.common.GovernanceDecision;
import com.project.optrabidz.governance.application.common.GovernanceRuleCode;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.application.port.AdminAuthorityQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class VisibilityEvaluator {
    private final AdminAuthorityQueryPort adminAuthorityQueryPort;

    public VisibilityEvaluator(AdminAuthorityQueryPort adminAuthorityQueryPort) {
        this.adminAuthorityQueryPort = adminAuthorityQueryPort;
    }

    public GovernanceDecision evaluateOwnedResourceVisibility(Long requesterAccountId,
                                                              RoleType requesterRole,
                                                              Long ownerAccountId) {
        Assert.notNull(requesterAccountId, "requesterAccountId must not be null");
        Assert.notNull(requesterRole, "requesterRole must not be null");
        Assert.notNull(ownerAccountId, "ownerAccountId must not be null");

        if (requesterRole == RoleType.ADMIN) {
            if (adminAuthorityQueryPort.isActiveAdmin(requesterAccountId)) {
                return GovernanceDecision.allow("Active admin may view governed resource");
            }
            return GovernanceDecision.deny(
                    GovernanceRuleCode.ADMIN_AUTHORITY_REQUIRED,
                    "admin",
                    "Only the active admin authority may view governed admin resources"
            );
        }

        if (requesterAccountId.equals(ownerAccountId)) {
            return GovernanceDecision.allow("Actor may view own resource");
        }

        return GovernanceDecision.deny(
                GovernanceRuleCode.NEUTRALITY_VIOLATION,
                "ownership",
                "Actor may view only owned resources unless a specific visibility policy allows otherwise"
        );
    }

    public GovernanceDecision evaluatePublicListingVisibility() {
        return GovernanceDecision.allow("Public listing visibility is allowed by default policy");
    }
}

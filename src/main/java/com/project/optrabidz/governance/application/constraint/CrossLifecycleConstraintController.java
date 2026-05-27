package com.project.optrabidz.governance.application.constraint;

import com.project.optrabidz.governance.application.common.GovernanceDecision;
import com.project.optrabidz.governance.application.common.GovernanceRuleCode;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class CrossLifecycleConstraintController {
    public GovernanceDecision evaluateBidSubmission(String listingState) {
        Assert.hasText(listingState, "listingState must not be blank");

        if (!"OPEN".equals(listingState)) {
            return GovernanceDecision.deny(
                    GovernanceRuleCode.LIFECYCLE_RULE_FAILED,
                    "listingState",
                    "Bids can be submitted only for OPEN listings"
            );
        }

        return GovernanceDecision.allow("Listing can accept bids");
    }

    public GovernanceDecision evaluateAgreementCreation(String bidState) {
        Assert.hasText(bidState, "bidState must not be blank");

        if (!"ACCEPTED".equals(bidState)) {
            return GovernanceDecision.deny(
                    GovernanceRuleCode.LIFECYCLE_RULE_FAILED,
                    "bidState",
                    "Agreement can be created only from an ACCEPTED bid"
            );
        }

        return GovernanceDecision.allow("Accepted bid can create agreement");
    }
}

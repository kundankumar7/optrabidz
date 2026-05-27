package com.project.optrabidz.governance.application.constraint;

import com.project.optrabidz.governance.application.common.GovernanceDecision;
import com.project.optrabidz.governance.application.eligibility.EligibilityEvaluationService;
import org.springframework.stereotype.Service;

@Service
public class EligibilityEvaluationController {
    private final EligibilityEvaluationService eligibilityEvaluationService;

    public EligibilityEvaluationController(EligibilityEvaluationService eligibilityEvaluationService) {
        this.eligibilityEvaluationService = eligibilityEvaluationService;
    }

    public GovernanceDecision evaluateStartupListingEligibility(Long accountId) {
        return eligibilityEvaluationService.evaluateStartupListingEligibility(accountId);
    }

    public GovernanceDecision evaluateInvestorBiddingEligibility(Long accountId) {
        return eligibilityEvaluationService.evaluateInvestorBiddingEligibility(accountId);
    }

    public void assertStartupCanPublishListing(Long accountId) {
        eligibilityEvaluationService.assertStartupCanPublishListing(accountId);
    }

    public void assertInvestorCanSubmitBid(Long accountId) {
        eligibilityEvaluationService.assertInvestorCanSubmitBid(accountId);
    }
}

package com.project.optrabidz.governance.application.eligibility;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "optrabidz.governance.eligibility")
public class GovernanceEligibilityProperties {
    private boolean requireStartupClassificationForListing = true;
    private boolean requireInvestorPreferencesForBidding = true;

    public boolean isRequireStartupClassificationForListing() {
        return requireStartupClassificationForListing;
    }

    public void setRequireStartupClassificationForListing(boolean requireStartupClassificationForListing) {
        this.requireStartupClassificationForListing = requireStartupClassificationForListing;
    }

    public boolean isRequireInvestorPreferencesForBidding() {
        return requireInvestorPreferencesForBidding;
    }

    public void setRequireInvestorPreferencesForBidding(boolean requireInvestorPreferencesForBidding) {
        this.requireInvestorPreferencesForBidding = requireInvestorPreferencesForBidding;
    }
}

package com.project.optrabidz.governance.application.eligibility;

import com.project.optrabidz.classification.application.dto.response.InvestorPreferenceResponse;
import com.project.optrabidz.classification.application.dto.response.StartupClassificationResponse;
import com.project.optrabidz.classification.application.port.in.InvestorPreferenceQueryPort;
import com.project.optrabidz.classification.application.port.in.StartupClassificationQueryPort;
import com.project.optrabidz.governance.application.common.GovernanceDecision;
import com.project.optrabidz.governance.application.common.GovernanceException;
import com.project.optrabidz.governance.application.common.GovernanceRuleCode;
import com.project.optrabidz.governance.application.common.GovernanceViolation;
import com.project.optrabidz.identity.application.port.IdentityQueryPort;
import com.project.optrabidz.identity.application.query.AccountSnapshot;
import com.project.optrabidz.identity.domain.model.AccountState;
import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.domain.model.Investor;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EligibilityEvaluationService {
    private final IdentityQueryPort identityQueryPort;
    private final StartupRepository startupRepository;
    private final InvestorRepository investorRepository;
    private final StartupClassificationQueryPort startupClassificationQueryPort;
    private final InvestorPreferenceQueryPort investorPreferenceQueryPort;
    private final GovernanceEligibilityProperties properties;

    public EligibilityEvaluationService(IdentityQueryPort identityQueryPort,
                                        StartupRepository startupRepository,
                                        InvestorRepository investorRepository,
                                        StartupClassificationQueryPort startupClassificationQueryPort,
                                        InvestorPreferenceQueryPort investorPreferenceQueryPort,
                                        GovernanceEligibilityProperties properties) {
        this.identityQueryPort = identityQueryPort;
        this.startupRepository = startupRepository;
        this.investorRepository = investorRepository;
        this.startupClassificationQueryPort = startupClassificationQueryPort;
        this.investorPreferenceQueryPort = investorPreferenceQueryPort;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public GovernanceDecision evaluateStartupListingEligibility(Long accountId) {
        Assert.notNull(accountId, "accountId must not be null");

        List<GovernanceViolation> violations = new ArrayList<>();
        Optional<AccountSnapshot> account = validateAccount(accountId, RoleType.STARTUP, violations);
        Optional<Startup> startup = startupRepository.findByAccountId(accountId);
        if (startup.isEmpty()) {
            violations.add(violation(
                    GovernanceRuleCode.STARTUP_ACTOR_NOT_FOUND,
                    "startup",
                    "Startup actor must exist before publishing a listing"
            ));
        }

        if (startup.isPresent() && properties.isRequireStartupClassificationForListing()) {
            StartupClassificationResponse classifications =
                    startupClassificationQueryPort.getStartupClassifications(startup.get().getStartupId());
            if (classifications.classifications() == null || classifications.classifications().isEmpty()) {
                violations.add(violation(
                        GovernanceRuleCode.STARTUP_CLASSIFICATION_REQUIRED,
                        "classification",
                        "Startup must define at least one classification before publishing a listing"
                ));
            }
        }

        return GovernanceDecision.fromViolations(
                "Startup is eligible to publish listings",
                "Startup is not eligible to publish listings",
                violations
        );
    }

    @Transactional(readOnly = true)
    public GovernanceDecision evaluateInvestorBiddingEligibility(Long accountId) {
        Assert.notNull(accountId, "accountId must not be null");

        List<GovernanceViolation> violations = new ArrayList<>();
        validateAccount(accountId, RoleType.INVESTOR, violations);
        Optional<Investor> investor = investorRepository.findByAccountId(accountId);
        if (investor.isEmpty()) {
            violations.add(violation(
                    GovernanceRuleCode.INVESTOR_ACTOR_NOT_FOUND,
                    "investor",
                    "Investor actor must exist before submitting a bid"
            ));
        }

        if (investor.isPresent() && properties.isRequireInvestorPreferencesForBidding()) {
            InvestorPreferenceResponse preferences =
                    investorPreferenceQueryPort.getInvestorPreferences(investor.get().getInvestorId());
            if (preferences.preferences() == null || preferences.preferences().isEmpty()) {
                violations.add(violation(
                        GovernanceRuleCode.INVESTOR_PREFERENCE_REQUIRED,
                        "classification",
                        "Investor must define at least one preference before submitting a bid"
                ));
            }
        }

        return GovernanceDecision.fromViolations(
                "Investor is eligible to submit bids",
                "Investor is not eligible to submit bids",
                violations
        );
    }

    public void assertStartupCanPublishListing(Long accountId) {
        GovernanceDecision decision = evaluateStartupListingEligibility(accountId);
        if (!decision.allowed()) {
            throw new GovernanceException(decision);
        }
    }

    public void assertInvestorCanSubmitBid(Long accountId) {
        GovernanceDecision decision = evaluateInvestorBiddingEligibility(accountId);
        if (!decision.allowed()) {
            throw new GovernanceException(decision);
        }
    }

    private Optional<AccountSnapshot> validateAccount(Long accountId,
                                                      RoleType expectedRole,
                                                      List<GovernanceViolation> violations) {
        Optional<AccountSnapshot> account = identityQueryPort.findAccountById(accountId);
        if (account.isEmpty()) {
            violations.add(violation(
                    GovernanceRuleCode.ACCOUNT_NOT_FOUND,
                    "account",
                    "Account must exist"
            ));
            return Optional.empty();
        }

        AccountSnapshot snapshot = account.get();
        if (snapshot.roleType() != expectedRole) {
            violations.add(violation(
                    GovernanceRuleCode.ROLE_MISMATCH,
                    "role",
                    "Expected role " + expectedRole + " but found " + snapshot.roleType()
            ));
        }

        if (snapshot.accountState() != AccountState.ACTIVE) {
            violations.add(violation(
                    GovernanceRuleCode.ACCOUNT_NOT_ACTIVE,
                    "accountState",
                    "Account must be ACTIVE"
            ));
        }

        if (snapshot.profileStatus() != ProfileStatus.COMPLETE) {
            violations.add(violation(
                    GovernanceRuleCode.PROFILE_INCOMPLETE,
                    "profileStatus",
                    "Profile must be COMPLETE"
            ));
        }

        return account;
    }

    private GovernanceViolation violation(GovernanceRuleCode ruleCode, String context, String message) {
        return new GovernanceViolation(ruleCode, context, message);
    }
}

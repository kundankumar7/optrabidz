package com.project.optrabidz.participation.application.profile;

import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.domain.model.Investor;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InvestorProfileCompletenessStrategy implements ParticipationProfileCompletenessStrategy {
    private final InvestorRepository investorRepository;

    public InvestorProfileCompletenessStrategy(InvestorRepository investorRepository) {
        this.investorRepository = investorRepository;
    }

    @Override
    public boolean supports(RoleType roleType) {
        return roleType == RoleType.INVESTOR;
    }

    @Override
    public ProfileStatus evaluate(Long accountId) {
        return investorRepository.findByAccountId(accountId)
                .filter(this::isComplete)
                .map(investor -> ProfileStatus.COMPLETE)
                .orElse(ProfileStatus.INCOMPLETE);
    }

    private boolean isComplete(Investor investor) {
        return hasText(investor.getPublicDisplayName())
                && hasText(investor.getInvestorDescription())
                && hasText(investor.getLegalEntityName())
                && !investor.getWebPresences().isEmpty()
                && investor.getWebPresences().stream().allMatch(this::hasText);
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}

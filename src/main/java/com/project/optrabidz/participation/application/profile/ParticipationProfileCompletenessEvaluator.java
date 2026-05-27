package com.project.optrabidz.participation.application.profile;

import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.RoleType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParticipationProfileCompletenessEvaluator {
    private final List<ParticipationProfileCompletenessStrategy> strategies;

    public ParticipationProfileCompletenessEvaluator(List<ParticipationProfileCompletenessStrategy> strategies) {
        this.strategies = strategies;
    }

    public ProfileStatus evaluate(Long accountId, RoleType roleType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(roleType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No profile completeness strategy for role: " + roleType))
                .evaluate(accountId);
    }
}

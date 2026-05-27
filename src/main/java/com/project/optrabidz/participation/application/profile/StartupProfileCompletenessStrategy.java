package com.project.optrabidz.participation.application.profile;

import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.model.StartupLegalRegistration;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StartupProfileCompletenessStrategy implements ParticipationProfileCompletenessStrategy {
    private final StartupRepository startupRepository;

    public StartupProfileCompletenessStrategy(StartupRepository startupRepository) {
        this.startupRepository = startupRepository;
    }

    @Override
    public boolean supports(RoleType roleType) {
        return roleType == RoleType.STARTUP;
    }

    @Override
    public ProfileStatus evaluate(Long accountId) {
        return startupRepository.findByAccountId(accountId)
                .filter(this::isComplete)
                .map(startup -> ProfileStatus.COMPLETE)
                .orElse(ProfileStatus.INCOMPLETE);
    }

    private boolean isComplete(Startup startup) {
        return hasText(startup.getLegalEntityName())
                && hasText(startup.getIncorporationCountryCode())
                && hasText(startup.getPublicDisplayName())
                && hasText(startup.getBusinessDescription())
                && !startup.getWebPresences().isEmpty()
                && startup.getWebPresences().stream().allMatch(this::hasText)
                && !startup.getLegalRegistrations().isEmpty()
                && startup.getLegalRegistrations().stream().allMatch(this::hasValidRegistration);
    }

    private boolean hasValidRegistration(StartupLegalRegistration registration) {
        return registration != null
                && hasText(registration.type())
                && hasText(registration.value());
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}

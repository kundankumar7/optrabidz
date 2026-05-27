package com.project.optrabidz.classification.infrastructure.repository;

import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;
import com.project.optrabidz.classification.domain.repository.InvestorPreferenceRepository;
import com.project.optrabidz.classification.infrastructure.entity.InvestorPreferenceEntity;
import com.project.optrabidz.classification.infrastructure.mapper.ClassificationPersistenceMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InvestorPreferenceRepositoryAdapter implements InvestorPreferenceRepository {
    private final JpaInvestorPreferenceRepository jpaInvestorPreferenceRepository;
    private final ClassificationPersistenceMapper mapper;

    public InvestorPreferenceRepositoryAdapter(JpaInvestorPreferenceRepository jpaInvestorPreferenceRepository,
                                               ClassificationPersistenceMapper mapper) {
        this.jpaInvestorPreferenceRepository = jpaInvestorPreferenceRepository;
        this.mapper = mapper;
    }

    @Override
    public InvestorPreferenceProfile saveAll(InvestorPreferenceProfile profile) {
        jpaInvestorPreferenceRepository.deleteByInvestorId(profile.getInvestorId());
        jpaInvestorPreferenceRepository.flush();
        if (profile.getPreferences().isEmpty()) {
            return InvestorPreferenceProfile.establish(profile.getInvestorId(), List.of());
        }

        List<InvestorPreferenceEntity> saved = jpaInvestorPreferenceRepository.saveAll(
                profile.getPreferences().stream()
                        .map(preference -> mapper.toEntity(preference, profile.getInvestorId()))
                        .toList()
        );
        return mapper.toInvestorProfile(profile.getInvestorId(), saved);
    }

    @Override
    public Optional<InvestorPreferenceProfile> findByInvestorId(Long investorId) {
        List<InvestorPreferenceEntity> entities =
                jpaInvestorPreferenceRepository.findByInvestorIdOrderByPreferenceTypeAscPreferenceValueAsc(investorId);
        if (entities.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toInvestorProfile(investorId, entities));
    }
}

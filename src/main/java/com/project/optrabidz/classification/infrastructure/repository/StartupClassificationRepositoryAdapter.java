package com.project.optrabidz.classification.infrastructure.repository;

import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;
import com.project.optrabidz.classification.domain.repository.StartupClassificationRepository;
import com.project.optrabidz.classification.infrastructure.entity.StartupClassificationEntity;
import com.project.optrabidz.classification.infrastructure.mapper.ClassificationPersistenceMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StartupClassificationRepositoryAdapter implements StartupClassificationRepository {
    private final JpaStartupClassificationRepository jpaStartupClassificationRepository;
    private final ClassificationPersistenceMapper mapper;

    public StartupClassificationRepositoryAdapter(JpaStartupClassificationRepository jpaStartupClassificationRepository,
                                                  ClassificationPersistenceMapper mapper) {
        this.jpaStartupClassificationRepository = jpaStartupClassificationRepository;
        this.mapper = mapper;
    }

    @Override
    public StartupClassificationProfile saveAll(StartupClassificationProfile profile) {
        jpaStartupClassificationRepository.deleteByStartupId(profile.getStartupId());
        jpaStartupClassificationRepository.flush();
        if (profile.getClassifications().isEmpty()) {
            return StartupClassificationProfile.establish(profile.getStartupId(), List.of());
        }

        List<StartupClassificationEntity> saved = jpaStartupClassificationRepository.saveAll(
                profile.getClassifications().stream()
                        .map(classification -> mapper.toEntity(classification, profile.getStartupId()))
                        .toList()
        );
        return mapper.toStartupProfile(profile.getStartupId(), saved);
    }

    @Override
    public Optional<StartupClassificationProfile> findByStartupId(Long startupId) {
        List<StartupClassificationEntity> entities =
                jpaStartupClassificationRepository.findByStartupIdOrderByClassificationTypeAscClassificationValueAsc(startupId);
        if (entities.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toStartupProfile(startupId, entities));
    }
}

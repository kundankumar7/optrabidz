package com.project.optrabidz.classification.infrastructure.mapper;

import com.project.optrabidz.classification.domain.model.InvestorPreference;
import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;
import com.project.optrabidz.classification.domain.model.StartupClassification;
import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;
import com.project.optrabidz.classification.infrastructure.entity.InvestorPreferenceEntity;
import com.project.optrabidz.classification.infrastructure.entity.StartupClassificationEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClassificationPersistenceMapper {
    public StartupClassificationEntity toEntity(StartupClassification classification, Long startupId) {
        StartupClassificationEntity entity = new StartupClassificationEntity();
        entity.setStartupClassificationId(classification.getStartupClassificationId());
        entity.setStartupId(startupId);
        entity.setClassificationType(classification.getClassificationType());
        entity.setClassificationValue(classification.getClassificationValue());
        return entity;
    }

    public InvestorPreferenceEntity toEntity(InvestorPreference preference, Long investorId) {
        InvestorPreferenceEntity entity = new InvestorPreferenceEntity();
        entity.setInvestorPreferenceId(preference.getInvestorPreferenceId());
        entity.setInvestorId(investorId);
        entity.setPreferenceType(preference.getPreferenceType());
        entity.setPreferenceValue(preference.getPreferenceValue());
        return entity;
    }

    public StartupClassification toDomain(StartupClassificationEntity entity) {
        return new StartupClassification(
                entity.getStartupClassificationId(),
                entity.getClassificationType(),
                entity.getClassificationValue()
        );
    }

    public InvestorPreference toDomain(InvestorPreferenceEntity entity) {
        return new InvestorPreference(
                entity.getInvestorPreferenceId(),
                entity.getPreferenceType(),
                entity.getPreferenceValue()
        );
    }

    public StartupClassificationProfile toStartupProfile(Long startupId, List<StartupClassificationEntity> entities) {
        return StartupClassificationProfile.establish(
                startupId,
                entities.stream().map(this::toDomain).toList()
        );
    }

    public InvestorPreferenceProfile toInvestorProfile(Long investorId, List<InvestorPreferenceEntity> entities) {
        return InvestorPreferenceProfile.establish(
                investorId,
                entities.stream().map(this::toDomain).toList()
        );
    }
}

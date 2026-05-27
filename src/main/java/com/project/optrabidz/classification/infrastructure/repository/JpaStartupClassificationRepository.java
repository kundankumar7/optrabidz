package com.project.optrabidz.classification.infrastructure.repository;

import com.project.optrabidz.classification.infrastructure.entity.StartupClassificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaStartupClassificationRepository extends JpaRepository<StartupClassificationEntity, Long> {
    List<StartupClassificationEntity> findByStartupIdOrderByClassificationTypeAscClassificationValueAsc(Long startupId);

    void deleteByStartupId(Long startupId);
}

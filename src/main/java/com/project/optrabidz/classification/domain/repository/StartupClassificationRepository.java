package com.project.optrabidz.classification.domain.repository;

import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;

import java.util.Optional;

public interface StartupClassificationRepository {
    StartupClassificationProfile saveAll(StartupClassificationProfile profile);

    Optional<StartupClassificationProfile> findByStartupId(Long startupId);
}

package com.project.optrabidz.participation.domain.repository;

import com.project.optrabidz.participation.domain.model.Startup;

import java.util.Optional;

public interface StartupRepository {
    Startup save(Startup startup);

    Optional<Startup> findById(Long startupId);

    Optional<Startup> findByAccountId(Long accountId);

    boolean existsByAccountId(Long accountId);
}

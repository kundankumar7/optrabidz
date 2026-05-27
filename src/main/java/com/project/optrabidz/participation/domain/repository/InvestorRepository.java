package com.project.optrabidz.participation.domain.repository;

import com.project.optrabidz.participation.domain.model.Investor;

import java.util.Optional;

public interface InvestorRepository {
    Investor save(Investor investor);

    Optional<Investor> findById(Long investorId);

    Optional<Investor> findByAccountId(Long accountId);

    boolean existsByAccountId(Long accountId);
}

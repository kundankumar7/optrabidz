package com.project.optrabidz.classification.domain.repository;

import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;

import java.util.Optional;

public interface InvestorPreferenceRepository {
    InvestorPreferenceProfile saveAll(InvestorPreferenceProfile profile);

    Optional<InvestorPreferenceProfile> findByInvestorId(Long investorId);
}
